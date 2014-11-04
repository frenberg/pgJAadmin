/**
 * 
 */
package com.frenberg.pgJAdmin.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.postgresql.util.PSQLException;

import com.frenberg.pgJAdmin.db.ConnectionManager;
import com.frenberg.pgJAdmin.db.QueryExecutor;
import com.frenberg.pgJAdmin.gui.CodeComplete.Mode;

/**
 * @author frenberg
 *
 */
public class pgJFrame extends JFrame {

	private static final long serialVersionUID = 3484293122472901955L;
	protected ConnectionManager connectionManager = new ConnectionManager();
	protected UndoManager undoManager = new UndoManager();
	protected UndoAction undoAction;
	protected RedoAction redoAction;
	protected JTextPane queryTextPane;
	protected JTable outputTable;

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	@SuppressWarnings("serial")
	public pgJFrame(String title) throws HeadlessException {
		super(title);
		setBounds(100, 100, 800, 600);

		// MENU
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		ConnectAction connectAction = new ConnectAction();
		JMenuItem mntmConnect = new JMenuItem(connectAction);
		fileMenu.add(mntmConnect);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		fileMenu.add(mntmOpen);
		JMenuItem mntmSave = new JMenuItem("Save");
		fileMenu.add(mntmSave);
		JMenuItem mntmClose = new JMenuItem("Close");
		fileMenu.add(mntmClose);

		JMenu editMenu = new JMenu("Edit");
		menuBar.add(editMenu);

		undoAction = new UndoAction();
		JMenuItem mntmUndo = new JMenuItem(undoAction);
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask()));
		redoAction = new RedoAction();
		JMenuItem mntmRedo = new JMenuItem(redoAction);
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				KeyEvent.SHIFT_DOWN_MASK
						| Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		editMenu.add(mntmUndo);
		editMenu.add(mntmRedo);

		JMenuItem mntmCut = new JMenuItem("Cut");
		editMenu.add(mntmCut);

		JMenuItem mntmCopy = new JMenuItem("Copy");
		editMenu.add(mntmCopy);

		JMenuItem mntmPaste = new JMenuItem("Paste");
		editMenu.add(mntmPaste);

		JMenu queryMenu = new JMenu("Query");
		menuBar.add(queryMenu);

		JMenuItem mntmExecuteQuery = new JMenuItem("Execute query");
		mntmExecuteQuery.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit()
						.getMenuShortcutKeyMask()));
		mntmExecuteQuery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// todo fix a swing worker thread instead
					// new SwingWorker<Void, Void>() {
					// @Override
					// protected Void doInBackground() throws Exception {
					// loadData();
					// return null;
					// }
					// }.execute();
					DefaultTableModel m = new QueryExecutor(connectionManager)
							.executeQuery(queryTextPane.getText());
					outputTable.setModel(m);
				} catch (Exception e1) {
					if (e1 instanceof PSQLException) {
						JOptionPane.showMessageDialog(null,
								((PSQLException) e1).getMessage());
					} else {
						// show connection dialog
						new ConnectAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Connect"));
					}
				}
			}
		});
		queryMenu.add(mntmExecuteQuery);

		getContentPane().setLayout(new BorderLayout(0, 0));

		// SPLIT PANE
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		getContentPane().add(splitPane, BorderLayout.CENTER);

		queryTextPane = new JTextPane();
		queryTextPane.getDocument().addUndoableEditListener(
				new MyUndoableEditListener());
		final CodeComplete cc = new CodeComplete(queryTextPane);
		queryTextPane.getDocument().addDocumentListener(cc);
		InputMap im = queryTextPane.getInputMap();
		ActionMap am = queryTextPane.getActionMap();
		im.put(KeyStroke.getKeyStroke("ENTER"), "commit");
		am.put("commit", new AbstractAction() {

			public void actionPerformed(ActionEvent ev) {
				if (cc.getMode() == Mode.COMPLETION) {
					int pos = queryTextPane.getSelectionEnd();
					try {
						queryTextPane.getDocument()
								.insertString(pos, " ", null);
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					queryTextPane.setCaretPosition(pos + 1);
					cc.setMode(Mode.INSERT);
				} else {
					queryTextPane.replaceSelection("\n");
				}
			}

		});

		// OUTPUT TEXTAREA
		outputTable = new JTable();

		// ADD QUERY TEXTAREA TO SCROLLPANE
		JScrollPane queryScrollPane = new JScrollPane(queryTextPane);
		splitPane.setLeftComponent(queryScrollPane);

		// ADD OUTPUT TEXTAREA TO OTHER SCROLLPANE
		// JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
		JScrollPane outputScrollPane = new JScrollPane(outputTable);
		splitPane.setRightComponent(outputScrollPane);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		splitPane.setDividerLocation(.5f);

	}

	protected class MyUndoableEditListener implements UndoableEditListener {
		public void undoableEditHappened(UndoableEditEvent e) {
			// Remember the edit and update the menus
			undoManager.addEdit(e.getEdit());
			undoAction.updateUndoState();
			redoAction.updateRedoState();
		}
	}

	protected class UndoAction extends AbstractAction {
		private static final long serialVersionUID = -7585936496429419608L;

		public UndoAction() {
			super("Undo");
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				undoManager.undo();
			} catch (CannotUndoException ex) {
				System.out.println("Unable to undo: " + ex);
				ex.printStackTrace();
			}
			updateUndoState();
			redoAction.updateRedoState();

		}

		protected void updateUndoState() {
			if (undoManager.canUndo()) {
				setEnabled(true);
				putValue(Action.NAME, undoManager.getUndoPresentationName());
			} else {
				setEnabled(false);
				putValue(Action.NAME, "Undo");
			}
		}
	}

	protected class RedoAction extends AbstractAction {

		private static final long serialVersionUID = 7047804662988770565L;

		public RedoAction() {
			super("Redo");
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				undoManager.redo();
			} catch (CannotRedoException ex) {
				System.out.println("Unable to redo: " + ex);
				ex.printStackTrace();
			}
			updateRedoState();
			undoAction.updateUndoState();

		}

		protected void updateRedoState() {
			if (undoManager.canRedo()) {
				setEnabled(true);
				putValue(Action.NAME, undoManager.getRedoPresentationName());
			} else {
				setEnabled(false);
				putValue(Action.NAME, "Redo");
			}
		}

	}

	@SuppressWarnings("serial")
	protected class ConnectAction extends AbstractAction {

		public ConnectAction() {
			super("Connect");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			connectionManager.loadSettings();
			ConnectionForm connectionFormFrame = new ConnectionForm();
			connectionFormFrame.getUserField().setText(connectionManager.getUser());
			connectionFormFrame.getPasswordField().setText(connectionManager.getPassword());
			connectionFormFrame.getSchemaField().setText(connectionManager.getSchema());
			if (connectionManager.getConnectionString() != null && !"".equals(connectionManager.getConnectionString())) {
				connectionFormFrame.getConnectionStringField().setText(connectionManager.getConnectionString());
			} else {
				connectionFormFrame.getConnectionStringField().setText("jdbc:postgresql://[host]:[port]/[database]");
			}
			
			
			JDialog connectionDialog = new JDialog(JOptionPane.getFrameForComponent(queryTextPane), Dialog.ModalityType.APPLICATION_MODAL);
			connectionDialog.setContentPane(connectionFormFrame);
			connectionDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			connectionDialog.setSize(450, 200);
			connectionDialog.setVisible(true);

			if (connectionFormFrame.useValues()) {
				System.out.println("using values");
				connectionManager.setUser(connectionFormFrame.getUserField().getText());
				connectionManager.setPassword(connectionFormFrame.getPasswordField().getText());
				connectionManager.setConnectionString(connectionFormFrame.getConnectionStringField().getText());
				connectionManager.setSchema(connectionFormFrame.getSchemaField().getText());
				try {
					connectionManager.saveSettings();
				} catch (IOException e1) {
					e1.printStackTrace();
					JOptionPane.showConfirmDialog(rootPane, e1.getMessage(), "Failed to save settings", JOptionPane.OK_OPTION);
				}
			} else {
				System.out.println("Not using values");
				
			}
			
			connectionDialog.dispose();
		}

	}
}