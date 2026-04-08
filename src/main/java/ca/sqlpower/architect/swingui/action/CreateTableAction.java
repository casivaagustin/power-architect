/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.architect.swingui.action;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.DatabaseMetaData;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.AbstractPlacer;
import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.TableEditPanel;
import ca.sqlpower.architect.swingui.TablePane;
import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.UserDefinedSQLType;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

/**
 * Action for creating a table and putting it in both the business
 * model and the play pen.
 */
public class CreateTableAction extends AbstractArchitectAction {
	private static final Logger logger = Logger.getLogger(CreateTableAction.class);
	
	// The default name of a table when it is initially created.
	private static final String NEW_TABLE_NAME = "New_Table";

	public CreateTableAction(ArchitectFrame frame) {
        super(frame,
                Messages.getString("CreateTableAction.name"), //$NON-NLS-1$
                Messages.getString("CreateTableAction.description"), //$NON-NLS-1$
                "new_table"); //$NON-NLS-1$
		putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_T,0));
	}

	public void actionPerformed(ActionEvent evt) {
		PlayPen playpen = getPlaypen();
        playpen.fireCancel();
		SQLTable t = null; 
		t = new SQLTable();
		t.initFolders(true);
		
		playpen.resetTableNames();
		int suffix = playpen.uniqueTableSuffix(NEW_TABLE_NAME);
		if (suffix != 0) {
		    t.setName(NEW_TABLE_NAME + "_" + suffix);
		} else {
		    t.setName(NEW_TABLE_NAME);
		}
		
		TablePane tp = new TablePane(t, playpen.getContentPane());
		TablePlacer tablePlacer = new TablePlacer(playpen, tp);
		try {
		    Point screenPos = MouseInfo.getPointerInfo().getLocation();
		    SwingUtilities.convertPointFromScreen(screenPos, playpen);
		    playpen.unzoomPoint(screenPos);
		    DataEntryPanel editPanel = tablePlacer.place(screenPos);
		    Window owner = SwingUtilities.getWindowAncestor(playpen);
		    JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
		            editPanel, owner,
		            Messages.getString("CreateTableAction.tablePropertiesDialogTitle"), //$NON-NLS-1$
		            "OK"); //$NON-NLS-1$
		    d.pack();
		    d.setLocationRelativeTo(owner);
		    d.setVisible(true);
		} catch (SQLObjectException ex) {
		    logger.error("Failed to place table:", ex); //$NON-NLS-1$
		    JOptionPane.showMessageDialog(playpen, ex.getMessage());
		}
	}
	
    private void addDefaultColumns(SQLTable table, ArchitectSwingSession session) throws SQLObjectException {
        UserDefinedSQLType bigintType = null;
        UserDefinedSQLType timestampType = null;
        for (UserDefinedSQLType type : session.getSQLTypes()) {
            if (bigintType == null && "BIGINT".equalsIgnoreCase(type.getName())) {
                bigintType = type;
            } else if (timestampType == null && "TIMESTAMP".equalsIgnoreCase(type.getName())) {
                timestampType = type;
            }
            if (bigintType != null && timestampType != null) break;
        }

        // id: BIGINT, primary key, not null
        SQLColumn id = bigintType != null ? new SQLColumn(bigintType) : new SQLColumn();
        id.setName("id");
        id.setNullable(DatabaseMetaData.columnNoNulls);
        table.addColumn(id);
        table.addToPK(id);

        // created_at: TIMESTAMP, not null, default now()
        SQLColumn createdAt = timestampType != null ? new SQLColumn(timestampType) : new SQLColumn();
        createdAt.setName("created_at");
        createdAt.setNullable(DatabaseMetaData.columnNoNulls);
        createdAt.setDefaultValue("now()");
        table.addColumn(createdAt);

        // updated_at: TIMESTAMP, nullable, default null
        SQLColumn updatedAt = timestampType != null ? new SQLColumn(timestampType) : new SQLColumn();
        updatedAt.setName("updated_at");
        updatedAt.setNullable(DatabaseMetaData.columnNullable);
        updatedAt.setDefaultValue(null);
        table.addColumn(updatedAt);
    }

	private class TablePlacer extends AbstractPlacer {
	    
	    private final TablePane tp;

	    TablePlacer(PlayPen playpen, TablePane tp) {
	        super(playpen);
	        this.tp = tp;
	    }
	    
        @Override
        protected String getEditDialogTitle() {
            return Messages.getString("CreateTableAction.tablePropertiesDialogTitle"); //$NON-NLS-1$
        }

        @Override
        public DataEntryPanel place(final Point p) throws SQLObjectException {
            TableEditPanel editPanel = new TableEditPanel(playpen.getSession(), tp) {
                @Override
                public boolean applyChanges() {
                    String warnings = generateWarnings();
                    if (warnings.length() == 0) {
                        ArchitectSwingSession session = getSession();
                        try {       
                            session.getWorkspace().begin("Creating a SQLTable and TablePane");
                            if (super.applyChanges()) {
                                tp.setName(tp.getModel().getName());
                                session.getTargetDatabase().addChild(tp.getModel());
                                addDefaultColumns(tp.getModel(), session);
                                playpen.selectNone();
                                playpen.addTablePane(tp, p);
                                tp.setSelected(true, SelectionEvent.SINGLE_SELECT);
                                session.getWorkspace().commit();
                                return true;
                            } else {
                                session.getWorkspace().rollback("Error creating table and table pane");
                                return false;
                            }
                        } catch (Throwable t) {
                            session.getWorkspace().rollback("Error creating table and table pane");
                            throw new RuntimeException(t);
                        }
                    } else {
                        JOptionPane.showMessageDialog(getPanel(),warnings);
                        //this is done so we can go back to this dialog after the error message
                        return false;
                    }
                }
            };

            editPanel.setNameText(tp.getModel().getName());
            editPanel.setPhysicalNameText(tp.getModel().getPhysicalName());
            editPanel.setPkNameText(tp.getModel().getName() + "_pk");
            
            return editPanel;
        }
	}
}
