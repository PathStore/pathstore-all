/**********
 *
 * Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ***********/
/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package pathstore.test;

/*
 * TableDialogEditDemo.java requires these files:
 *   ColorRenderer.java
 *   ColorEditor.java
 */

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * This is like TableDemo, except that it substitutes a
 * Favorite Color column for the Last Name column and specifies
 * a custom cell renderer and editor for the color data.
 * TODO: Comment
 */
public class TableDemo extends JPanel implements TableModelListener, ActionListener {
    private boolean DEBUG = false;

    private JTable table = null;
    private UpdateThread updateThread = null;
    private JTextField name = null;
    private JTextField sport = null;


    public TableDemo() {
        super(new GridLayout(3, 1));

        table = new JTable(new MyTableModel());

        table.getModel().addTableModelListener(this);

        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Set up renderer and editor for the Favorite Color column.
        table.setDefaultRenderer(Color.class,
                new ColorRenderer(true));
        table.setDefaultEditor(Color.class,
                new ColorEditor());

        //Add the scroll pane to this panel.
        add(scrollPane);

        JPanel userPanel = new JPanel();

        JLabel lname = new JLabel("Name");
        name = new JTextField(20);
        JLabel lsport = new JLabel("Sport");
        sport = new JTextField(20);

        JButton newUser = new JButton("Create");
        newUser.setActionCommand("NewUser");
        newUser.addActionListener(this);

        userPanel.add(lname);
        userPanel.add(name);
        userPanel.add(lsport);
        userPanel.add(sport);
        userPanel.add(newUser);

        add(userPanel);


        JPanel create10Panel = new JPanel();

        JButton create10User = new JButton("Create 10");
        create10User.setActionCommand("Create10");
        create10User.addActionListener(this);

        create10Panel.add(create10User);

        add(create10Panel);

        updateThread = new UpdateThread(this);
        updateThread.start();
    }

    public void reloadModel() {
        ((MyTableModel) table.getModel()).loadModel();
        this.repaint();
    }


    class UpdateThread extends Thread {
        TableDemo tableDemo;

        public UpdateThread(TableDemo tableDemo) {
            this.tableDemo = tableDemo;
        }

        synchronized public void run() {

            while (true) {
                try {
                    tableDemo.reloadModel();

                    this.wait(1000);
                } catch (InterruptedException e) {
                    System.err.println("TableDemo exception: " + e.toString());
                    e.printStackTrace();
                }
            }
        }

    }

    class MyTableModel extends AbstractTableModel {

        private String[] columnNames = {"name",
                "color",
                "sport",
                "years",
                "vegetarian",
                "Delete"};
        private Object[][] data = {
                {"Mary", new Color(153, 0, 153),
                        "Snowboarding", new Integer(5), new Boolean(false)},
                {"Alison", new Color(51, 51, 153),
                        "Rowing", new Integer(3), new Boolean(true)},
                {"Kathy", new Color(51, 102, 51),
                        "Knitting", new Integer(2), new Boolean(false)},
                {"Sharon", Color.red,
                        "Speed reading", new Integer(20), new Boolean(true)},
                {"Philip", Color.pink,
                        "Pool", new Integer(10), new Boolean(false)}
        };


        public void insertRecords() {

            PathStoreCluster cluster = PathStoreCluster.getDaemonInstance();
            Session session = cluster.connect();

            for (int x = 0; x < data.length; x++) {
                Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
                insert.value("name", data[x][0]);

                Color color = (Color) data[x][1];

                List<Integer> rgb = new Vector<Integer>();

                rgb.add((int) color.getRed());
                rgb.add((int) color.getGreen());
                rgb.add((int) color.getBlue());

                insert.value("color", rgb);
                insert.value("sport", data[x][2]);
                insert.value("years", data[x][3]);
                insert.value("vegetarian", data[x][4]);

                session.execute(insert);
            }
        }

        public void updateRecord(int x, int y) {
            PathStoreCluster cluster = PathStoreCluster.getDaemonInstance();
            Session session = cluster.connect();
            Update update = QueryBuilder.update("pathstore_demo", "users");

            Color color = (Color) data[x][1];


            if (y == 1) {
                List<Integer> rgb = new Vector<Integer>();
                rgb.add((int) color.getRed());
                rgb.add((int) color.getGreen());
                rgb.add((int) color.getBlue());
                update.with(QueryBuilder.set(columnNames[y], rgb));

            } else
                update.with(QueryBuilder.set(columnNames[y], data[x][y]));

            update.where(QueryBuilder.eq("name", data[x][0]));
            session.execute(update);
        }

        public void deleteRecord(int x) {
            PathStoreCluster cluster = PathStoreCluster.getDaemonInstance();
            Session session = cluster.connect();

            Delete delete = QueryBuilder.delete().from("pathstore_demo", "users");
            delete.where(QueryBuilder.eq("name", data[x][0]));

            session.execute(delete);

        }


        public void loadModel() {
            PathStoreCluster cluster = PathStoreCluster.getDaemonInstance();
            Session session = cluster.connect();

            Select select = QueryBuilder.select().all().from("pathstore_demo", "users");

            ResultSet results = session.execute(select);

            Vector<Object[]> tuples = new Vector<Object[]>();

            for (Row row : results) {
                Object[] tuple = {"", null, "", null, null, null};

                tuple[0] = row.getString("name");
                List<Integer> rgb = row.getList("color", Integer.class);
                Color color = new Color(rgb.get(0), rgb.get(1), rgb.get(2));
                tuple[1] = color;
                tuple[2] = row.getString("sport");
                tuple[3] = new Integer(row.getInt("years"));
                tuple[4] = new Boolean(row.getBool("vegetarian"));
                tuple[5] = new Boolean(false);


                tuples.add(tuple);
            }

            data = new Object[tuples.size()][6];

            for (int x = 0; x < tuples.size(); x++)
                data[x] = tuples.elementAt(x);
        }

        public MyTableModel() {
            loadModel();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            if (DEBUG) {
                System.out.println("Setting value at " + row + "," + col
                        + " to " + value
                        + " (an instance of "
                        + value.getClass() + ")");
            }

            data[row][col] = value;
            fireTableCellUpdated(row, col);

            if (DEBUG) {
                System.out.println("New value of data:");
                printDebugData();
            }
        }

        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i = 0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j = 0; j < numCols; j++) {
                    System.out.print("  " + data[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }


    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("TableDialogEditDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new TableDemo();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setSize(400, 400);
        frame.setVisible(true);


    }

    private static void parseCommandLineArguments(String args[]) {
        Options options = new Options();

        options.addOption(Option.builder().longOpt("server")
                .desc("NUMBER")
                .hasArg()
                .argName("server")
                .build());

        options.addOption(Option.builder().longOpt("rmiport")
                .desc("NUMBER")
                .hasArg()
                .argName("PORT")
                .build());

        options.addOption(Option.builder().longOpt("cassandraport")
                .desc("NUMBER")
                .hasArg()
                .argName("PORT")
                .build());


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("rmiport"))
            PathStoreProperties.getInstance().RMIRegistryPort = Integer.parseInt(cmd.getOptionValue("rmiport"));

        if (cmd.hasOption("cassandraport"))
            PathStoreProperties.getInstance().CassandraPort = Integer.parseInt(cmd.getOptionValue("cassandraport"));

        if (cmd.hasOption("server")) {
            PathStoreProperties.getInstance().RMIRegistryIP = cmd.getOptionValue("server");
            PathStoreProperties.getInstance().CassandraIP = cmd.getOptionValue("server");
        }

        System.out.println(PathStoreProperties.getInstance().RMIRegistryIP);
    }


    public static void main(String[] args) {
        parseCommandLineArguments(args);
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });


    }

    @Override
    public void tableChanged(TableModelEvent e) {
        MyTableModel model = (MyTableModel) table.getModel();

        if (e.getColumn() == 5)
            model.deleteRecord(e.getFirstRow());
        else
            model.updateRecord(e.getFirstRow(), e.getColumn());

    }

    private void insertUser(Session session, String name, String sport,
                            List<Integer> rgb, int years, boolean vegetarian) {
        Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
        insert.value("name", name);
        insert.value("color", rgb);
        insert.value("sport", sport);
        insert.value("years", years);
        insert.value("vegetarian", vegetarian);
        session.execute(insert);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        PathStoreCluster cluster = PathStoreCluster.getDaemonInstance();
        Session session = cluster.connect();
        Random rand = new Random();

        List<Integer> rgb = new Vector<Integer>();
        rgb.add(rand.nextInt(255));
        rgb.add(rand.nextInt(255));
        rgb.add(rand.nextInt(255));

        switch (e.getActionCommand()) {
            case "NewUser":
                insertUser(session, name.getText(), sport.getText(), rgb, 0, false);
                break;
            case "Create10":

                for (int x = 0; x < 10; x++) {
                    String name = "node" + PathStoreProperties.getInstance().NodeID + "-u-" + rand.nextInt(Integer.MAX_VALUE);
                    String sport = "";
                    insertUser(session, name, sport, rgb, x, false);
                }
        }


    }
}
