/*
 * Copyright (C) 2017 LucidWolf
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package easydeltapie;

import easydeltapie.connect.DeltaComPort;
import easydeltapie.connect.PortScanner;
import easydeltapie.connect.controller.AutoLevelControl;
import easydeltapie.connect.controller.LimitCheckController;
import easydeltapie.connect.controller.SingleCommandControl;
import easydeltapie.connect.controller.SingleProbeControl;
import easydeltapie.connect.machine.states.EepromValue;
import easydeltapie.connect.machine.states.EndStopValue;
import easydeltapie.connect.machine.Firmware;
import easydeltapie.connect.machine.states.EepromState;
import easydeltapie.connect.machine.states.EndStopState;
import easydeltapie.connect.machine.states.PositionState;
import easydeltapie.escher3D.BedProbePoint;
import easydeltapie.escher3D.CalibrationException;
import easydeltapie.escher3D.DeltaCalibration;
import easydeltapie.escher3D.DeltaParameters;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public final class EasyDeltaPie extends javax.swing.JFrame {
    private final HashSet<Component> viewConnectRequired = new HashSet<>();
    private final HashSet<Component> viewIgnore = new HashSet<>();
    private final HashSet<Component> commandButtons = new HashSet<>();
    private final HashSet<Component> immuneToEnable = new HashSet<>();
    private LimitCheckController limitChecker;
    private final boolean debug = true;
    private boolean warmedUp;
    private StringBuilder summary_level = new StringBuilder();
    /**
     * Creates new form DeltaAutoLevel
     */
    public EasyDeltaPie() {
        initComponents();
        buildConnectRequired();
        updatedConnectedViews(false);
        // running on awt since should be fast and locking screen good probably
        java.awt.EventQueue.invokeLater(new PortScanner(this));
        
    }
    private void buildConnectRequired() {
        // components that are always active reguardless of connection
        viewIgnore.add(connectPanel);
        viewIgnore.add(button_Connect);
        // components that need connection to be active
        viewConnectRequired.add(gantryPanel);
        viewConnectRequired.add(setupPanel);
        viewConnectRequired.add(levelPanel);
        viewConnectRequired.add(detailPanel);
        viewConnectRequired.add(tolerancePanel);
        // connect panel bit required for voodoo
        viewConnectRequired.add(outputPanel);
        
        // buttons that get disabled during moves
        // connect buttons
        commandButtons.add(this.button_command);
        commandButtons.add(this.button_eeprom);
        // gantry buttons
        commandButtons.add(this.button_homeAll);
        commandButtons.add(this.button_Center);
        commandButtons.add(this.button_GoToCart);
        commandButtons.add(this.button_GoToRad);
        commandButtons.add(this.button_Xneg);
        commandButtons.add(this.button_Xpos);
        commandButtons.add(this.button_Yneg);
        commandButtons.add(this.button_Ypos);
        commandButtons.add(this.button_Zneg);
        commandButtons.add(this.button_Zpos);
        commandButtons.add(this.button_Zneg1);
        commandButtons.add(this.button_Zpos1);
        commandButtons.add(this.button_Zneg2);
        commandButtons.add(this.button_Zpos2);
        commandButtons.add(this.button_Rpos);
        commandButtons.add(this.button_Tpos);
        commandButtons.add(this.button_Rneg);
        commandButtons.add(this.button_Tneg);
        // buttons in autolevel
        commandButtons.add(this.button_autoLevel);
        // buttons in setup
        commandButtons.add(this.button_limitCheck);
        commandButtons.add(this.button_homeAll1);
        commandButtons.add(this.button_updateHeight);
        commandButtons.add(this.button_updatePrintRadius);
        commandButtons.add(this.button_updateProbeHeight);
        commandButtons.add(this.button_updateProbeStartHeight);
        commandButtons.add(this.button_updateRadiusAtZero);
        commandButtons.add(this.button_updateRodLength);
        commandButtons.add(this.button_startProbeCheck);
        commandButtons.add(this.button_setProbeUsingCurZ);
        // buttons in detail
        commandButtons.add(this.button_detailSavePoint);
        commandButtons.add(this.slider_detailPoint);
        commandButtons.add(this.slider_detailRadPer);
        


    }
    // called by connectWindowListener after eeprom read...
    public void updatedViews(){
        updatedConnectedViews(tabs, warmedUp);
        // update based on Detailed
        
    }
    public void updatedConnectedViews(boolean connected){
        this.warmedUp = connected;
        updatedViews();
    }
    // this is only for the tabs dont use this for other things
    private void updatedConnectedViews(Container tabs, boolean connected){
        Component[] components = tabs.getComponents();
        for (Component component : components) {
            if(viewIgnore.contains(component) && component instanceof Container){
                updatedConnectedViews((Container)component, connected);
            }else{
                // default to disabled for panels
                boolean enabled = !connected;
                // if based on connected then enable
                if(viewConnectRequired.contains(component)){
                    enabled = connected;
                }
                component.setEnabled(enabled);
                if (component instanceof Container) {
                    setComponentViews((Container)component, enabled);
                }
            }
        }
    }
    private void setComponentViews(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                setComponentViews((Container)component, enable);
            }
        }   
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        tabs = new javax.swing.JTabbedPane();
        connectPanel = new javax.swing.JPanel();
        inputPanel = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel50 = new javax.swing.JLabel();
        button_rescanComs = new javax.swing.JButton();
        combo_coms = new javax.swing.JComboBox();
        combo_baud = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        outputPanel = new javax.swing.JPanel();
        scroll_output = new javax.swing.JScrollPane();
        text_output = new javax.swing.JTextArea();
        check_filter = new javax.swing.JCheckBox();
        check_scroll = new javax.swing.JCheckBox();
        text_command = new javax.swing.JTextField();
        button_command = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        combo_eeprom = new javax.swing.JComboBox();
        text_eeprom = new javax.swing.JTextField();
        button_eeprom = new javax.swing.JButton();
        button_writeOut = new javax.swing.JButton();
        button_readIn = new javax.swing.JButton();
        button_Connect = new javax.swing.JToggleButton();
        setupPanel = new javax.swing.JPanel();
        jLabel64 = new javax.swing.JLabel();
        label_limit_A1 = new javax.swing.JLabel();
        label_limit_B1 = new javax.swing.JLabel();
        label_limit_C1 = new javax.swing.JLabel();
        label_limit_D1 = new javax.swing.JLabel();
        label_limit_A_state = new javax.swing.JLabel();
        label_limit_B_state = new javax.swing.JLabel();
        label_limit_C_state = new javax.swing.JLabel();
        label_limit_D_state = new javax.swing.JLabel();
        button_limitCheck = new javax.swing.JToggleButton();
        jLabel65 = new javax.swing.JLabel();
        text_setup_maxPrintRadius = new javax.swing.JTextField();
        button_updatePrintRadius = new javax.swing.JButton();
        jLabel66 = new javax.swing.JLabel();
        text_setup_height = new javax.swing.JTextField();
        button_updateHeight = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        text_gantryDesc1 = new javax.swing.JTextPane();
        button_homeAll1 = new javax.swing.JButton();
        jLabel67 = new javax.swing.JLabel();
        text_Nudge_Distance2 = new javax.swing.JSpinner();
        button_setProbeUsingCurZ = new javax.swing.JButton();
        button_Zpos2 = new javax.swing.JButton();
        button_Zneg2 = new javax.swing.JButton();
        toggle_allowNegativeOnProbe = new javax.swing.JToggleButton();
        button_startProbeCheck = new javax.swing.JButton();
        jLabel68 = new javax.swing.JLabel();
        text_setup_probeStartHeight = new javax.swing.JTextField();
        button_updateProbeStartHeight = new javax.swing.JButton();
        jLabel69 = new javax.swing.JLabel();
        text_posX2 = new javax.swing.JLabel();
        text_posY2 = new javax.swing.JLabel();
        text_posZ2 = new javax.swing.JLabel();
        jLabel70 = new javax.swing.JLabel();
        text_setup_rodLength = new javax.swing.JTextField();
        button_updateRodLength = new javax.swing.JButton();
        jLabel71 = new javax.swing.JLabel();
        text_setup_rodAtZero = new javax.swing.JTextField();
        button_updateRadiusAtZero = new javax.swing.JButton();
        jLabel72 = new javax.swing.JLabel();
        text_setup_probeHeight = new javax.swing.JTextField();
        button_updateProbeHeight = new javax.swing.JButton();
        levelPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jLabel13 = new javax.swing.JLabel();
        text_level_taps = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        text_level_cov = new javax.swing.JTextField();
        progress_level = new javax.swing.JProgressBar();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        scroll_deltaLevel = new javax.swing.JScrollPane();
        text_level_output = new javax.swing.JTextArea();
        button_autoLevel = new javax.swing.JButton();
        slider_radPer = new javax.swing.JSlider();
        detailPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jLabel21 = new javax.swing.JLabel();
        slider_detailRadPer = new javax.swing.JSlider();
        slider_detailPoint = new javax.swing.JSlider();
        jLabel10 = new javax.swing.JLabel();
        button_detailSavePoint = new javax.swing.JButton();
        button_calcTowerOffsets = new javax.swing.JButton();
        text_detailHeight = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        button_Zpos1 = new javax.swing.JButton();
        button_Zneg1 = new javax.swing.JButton();
        text_Nudge_Distance1 = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        button_detailGoto = new javax.swing.JButton();
        jLabel59 = new javax.swing.JLabel();
        text_detail_p1 = new javax.swing.JLabel();
        text_detail_p2 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        text_detail_p3 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        text_detail_p4 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        text_posY1 = new javax.swing.JLabel();
        text_posX1 = new javax.swing.JLabel();
        text_posZ1 = new javax.swing.JLabel();
        toggle_AllowNegHeightDetail = new javax.swing.JToggleButton();
        button_detail_clearAll = new javax.swing.JButton();
        button_detail_Ap = new javax.swing.JButton();
        button_detail_Am = new javax.swing.JButton();
        button_detail_Bp = new javax.swing.JButton();
        button_detail_Bm = new javax.swing.JButton();
        button_detail_setAllZero = new javax.swing.JButton();
        button_detail_Cp = new javax.swing.JButton();
        button_detail_Cm = new javax.swing.JButton();
        button_detail_Rp = new javax.swing.JButton();
        button_detail_Rm = new javax.swing.JButton();
        jLabel53 = new javax.swing.JLabel();
        text_detail_TowerTweak = new javax.swing.JTextField();
        button_homeAll2 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        tolerancePanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        text_cal_curStepsMM = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        text_cal_zExpected = new javax.swing.JTextField();
        jLabel33 = new javax.swing.JLabel();
        text_cal_zActual = new javax.swing.JTextField();
        button_cal_step = new javax.swing.JButton();
        jLabel31 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        text_cal_stepsAdjusted = new javax.swing.JLabel();
        button_cal_updateSteps = new javax.swing.JButton();
        jLabel55 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        text_cal_horzRad = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        text_cal_rodA = new javax.swing.JLabel();
        text_cal_rodB = new javax.swing.JLabel();
        text_cal_rodC = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        text_cal_radius = new javax.swing.JTextField();
        jLabel36 = new javax.swing.JLabel();
        text_cal_radiusA = new javax.swing.JTextField();
        button_cal_Rods = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        text_cal_angleA = new javax.swing.JTextField();
        jLabel43 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        text_cal_angleB = new javax.swing.JTextField();
        button_cal_updateTowerAngle = new javax.swing.JButton();
        text_cal_angleC = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        text_cal_radiusB = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        text_cal_radiusC = new javax.swing.JTextField();
        jLabel39 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        text_cal_calcA = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        text_cal_calcB = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        text_cal_calcC = new javax.swing.JLabel();
        button_cal_updateRods = new javax.swing.JButton();
        jLabel56 = new javax.swing.JLabel();
        gantryPanel = new javax.swing.JPanel();
        motionPanel = new javax.swing.JPanel();
        button_Ypos = new javax.swing.JButton();
        button_Center = new javax.swing.JButton();
        button_Xneg = new javax.swing.JButton();
        button_Xpos = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        button_Zpos = new javax.swing.JButton();
        button_Zneg = new javax.swing.JButton();
        button_Yneg = new javax.swing.JButton();
        text_Nudge_Distance = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        text_posX = new javax.swing.JTextField();
        text_posY = new javax.swing.JTextField();
        text_posZ = new javax.swing.JTextField();
        text_posR = new javax.swing.JTextField();
        text_posT = new javax.swing.JTextField();
        button_GoToCart = new javax.swing.JButton();
        button_GoToRad = new javax.swing.JButton();
        button_Rneg = new javax.swing.JButton();
        button_Rpos = new javax.swing.JButton();
        button_Tpos = new javax.swing.JButton();
        button_Tneg = new javax.swing.JButton();
        button_homeAll = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        mainPanel.setPreferredSize(new java.awt.Dimension(800, 700));

        tabs.setMinimumSize(new java.awt.Dimension(800, 600));
        tabs.setPreferredSize(new java.awt.Dimension(800, 600));

        connectPanel.setPreferredSize(new java.awt.Dimension(795, 700));

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Repetier", " " }));

        jLabel50.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel50.setText("Firmware");

        button_rescanComs.setText("Rescan");
        button_rescanComs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_rescanComsActionPerformed(evt);
            }
        });

        combo_coms.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Scanning" }));

        combo_baud.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "250000", "115200", "230400", "38400", "57600", "76800" }));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Baud");

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("COM");

        javax.swing.GroupLayout inputPanelLayout = new javax.swing.GroupLayout(inputPanel);
        inputPanel.setLayout(inputPanelLayout);
        inputPanelLayout.setHorizontalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputPanelLayout.createSequentialGroup()
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(inputPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(combo_coms, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(combo_baud, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(button_rescanComs)
                    .addGroup(inputPanelLayout.createSequentialGroup()
                        .addComponent(jLabel50, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 257, Short.MAX_VALUE))
        );
        inputPanelLayout.setVerticalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputPanelLayout.createSequentialGroup()
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(combo_coms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_rescanComs))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(combo_baud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel50, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 19, Short.MAX_VALUE))
        );

        text_output.setEditable(false);
        text_output.setColumns(20);
        text_output.setRows(5);
        scroll_output.setViewportView(text_output);

        check_filter.setSelected(true);
        check_filter.setText("Filter Noise");

        check_scroll.setSelected(true);
        check_scroll.setText("Scroll Lock");

        button_command.setText("Send");
        button_command.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_commandActionPerformed(evt);
            }
        });

        jLabel4.setText("Edit Eeprom Settings (WARNING: CAN MESS UP CONFIGURATION)");

        combo_eeprom.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Waiting" }));
        combo_eeprom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combo_eepromActionPerformed(evt);
            }
        });

        button_eeprom.setText("Update");
        button_eeprom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_eepromActionPerformed(evt);
            }
        });

        button_writeOut.setText("Write Out Settings To File");
        button_writeOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_writeOutActionPerformed(evt);
            }
        });

        button_readIn.setText("Read In Setting From File");
        button_readIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_readInActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout outputPanelLayout = new javax.swing.GroupLayout(outputPanel);
        outputPanel.setLayout(outputPanelLayout);
        outputPanelLayout.setHorizontalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputPanelLayout.createSequentialGroup()
                        .addComponent(check_filter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(check_scroll)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(outputPanelLayout.createSequentialGroup()
                        .addComponent(text_command, javax.swing.GroupLayout.PREFERRED_SIZE, 696, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_command, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, outputPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(scroll_output, javax.swing.GroupLayout.PREFERRED_SIZE, 765, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, outputPanelLayout.createSequentialGroup()
                        .addComponent(combo_eeprom, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_eeprom, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_eeprom))
                    .addGroup(outputPanelLayout.createSequentialGroup()
                        .addComponent(button_writeOut, javax.swing.GroupLayout.PREFERRED_SIZE, 374, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_readIn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        outputPanelLayout.setVerticalGroup(
            outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputPanelLayout.createSequentialGroup()
                .addComponent(scroll_output, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_command)
                    .addComponent(text_command, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(check_filter)
                    .addComponent(check_scroll))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_eeprom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(text_eeprom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_eeprom))
                .addGap(18, 18, 18)
                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_writeOut)
                    .addComponent(button_readIn))
                .addContainerGap(175, Short.MAX_VALUE))
        );

        button_Connect.setText("Connect/Disconnect");
        button_Connect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_ConnectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout connectPanelLayout = new javax.swing.GroupLayout(connectPanel);
        connectPanel.setLayout(connectPanelLayout);
        connectPanelLayout.setHorizontalGroup(
            connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(connectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(connectPanelLayout.createSequentialGroup()
                        .addComponent(outputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(connectPanelLayout.createSequentialGroup()
                        .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(inputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(button_Connect))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        connectPanelLayout.setVerticalGroup(
            connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(connectPanelLayout.createSequentialGroup()
                .addComponent(inputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(button_Connect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18))
        );

        tabs.addTab("Connect", connectPanel);

        setupPanel.setMaximumSize(new java.awt.Dimension(800, 600));
        setupPanel.setMinimumSize(new java.awt.Dimension(800, 600));

        jLabel64.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel64.setText("Limit Switches");

        label_limit_A1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        label_limit_A1.setText("A");

        label_limit_B1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        label_limit_B1.setText("B");

        label_limit_C1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        label_limit_C1.setText("C");

        label_limit_D1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        label_limit_D1.setText("Tip");

        label_limit_A_state.setBackground(java.awt.Color.green);
        label_limit_A_state.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label_limit_A_state.setText("OPEN");
        label_limit_A_state.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        label_limit_A_state.setOpaque(true);

        label_limit_B_state.setBackground(java.awt.Color.green);
        label_limit_B_state.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label_limit_B_state.setText("OPEN");
        label_limit_B_state.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        label_limit_B_state.setOpaque(true);

        label_limit_C_state.setBackground(java.awt.Color.green);
        label_limit_C_state.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label_limit_C_state.setText("OPEN");
        label_limit_C_state.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        label_limit_C_state.setOpaque(true);

        label_limit_D_state.setBackground(java.awt.Color.green);
        label_limit_D_state.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label_limit_D_state.setText("OPEN");
        label_limit_D_state.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        label_limit_D_state.setOpaque(true);

        button_limitCheck.setText("Check");
        button_limitCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_limitCheckActionPerformed(evt);
            }
        });

        jLabel65.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel65.setText("Max Print Radius");

        button_updatePrintRadius.setText("Update Eeprom");
        button_updatePrintRadius.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updatePrintRadiusActionPerformed(evt);
            }
        });

        jLabel66.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel66.setText("Measured Height");

        button_updateHeight.setText("Update Eeprom");
        button_updateHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updateHeightActionPerformed(evt);
            }
        });

        text_gantryDesc1.setEditable(false);
        text_gantryDesc1.setText("Inital Setup Checklist\n1.) Click the Check button.\n  1a.) The Switches should default to Open if not check firmware\n  1b.) Push the stops on your machine and verify they change to closed\n  1c.) Unclick the Check button to stop testing\n2.) Click the Home All button\n  2a.) Physicaly measure the distance from the tip to the bed and verify Measured Height\n  2b.) If height is off by more that 5mm then enter in measured height and click update.\n  2c.) Verify values for diagonal rod, max print radius, and horizontal radius are close\n  2d.) Verify probe height is withing 1 mm \n\t(Force sensor or nozzle detection use zero) \n\tIf probe is below the tip when added use posative value\n3.) You are now ready to run the Probe Check\n 3a.) The probe check will do a probe at 0,0 and set the heigh based on that probe\n 3b.) You will then manualy move the probe down to where first layer height actual\n 3c.) If you need more travel. Toggle negative motion. DANGER YOU CAN CRASH HEAD. (Motion limited to 0.5mm)\n 3d.) Then click update probe offset.\n 3e.) Repeat steps a - d until the probe height matches your manual height (probe offset complete)\n4.) You are now ready for Auto Level");
        text_gantryDesc1.setToolTipText("");
        jScrollPane4.setViewportView(text_gantryDesc1);

        button_homeAll1.setText("Home All");
        button_homeAll1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_homeAll1ActionPerformed(evt);
            }
        });

        jLabel67.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel67.setText("Step Size mm");
        jLabel67.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        text_Nudge_Distance2.setModel(new javax.swing.SpinnerListModel(new String[] {".01", ".03", ".05", ".10", ".30", ".50", "1.0", "5.0"}));

        button_setProbeUsingCurZ.setText("Update Probe Offset");
        button_setProbeUsingCurZ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_setProbeUsingCurZActionPerformed(evt);
            }
        });

        button_Zpos2.setText("+Z");
        button_Zpos2.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Zpos2.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Zpos2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_Zpos2ActionPerformed(evt);
            }
        });

        button_Zneg2.setText("-Z");
        button_Zneg2.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Zneg2.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Zneg2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_Zneg2ActionPerformed(evt);
            }
        });

        toggle_allowNegativeOnProbe.setText("Allow Negative Motion (Danger)");
        toggle_allowNegativeOnProbe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggle_allowNegativeOnProbeActionPerformed(evt);
            }
        });

        button_startProbeCheck.setText("Probe Check Begin (Hand on Kill Switch)");
        button_startProbeCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_startProbeCheckActionPerformed(evt);
            }
        });

        jLabel68.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel68.setText("Probe Start Height");

        button_updateProbeStartHeight.setText("Update Eeprom");
        button_updateProbeStartHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updateProbeStartHeightActionPerformed(evt);
            }
        });

        jLabel69.setText("Current Pos");

        text_posX2.setText("na");

        text_posY2.setText("na");

        text_posZ2.setText("na");

        jLabel70.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel70.setText("Rod Length");

        button_updateRodLength.setText("Update Eeprom");
        button_updateRodLength.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updateRodLengthActionPerformed(evt);
            }
        });

        jLabel71.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel71.setText("Rod Radius at zero");

        button_updateRadiusAtZero.setText("Update Eeprom");
        button_updateRadiusAtZero.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updateRadiusAtZeroActionPerformed(evt);
            }
        });

        jLabel72.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel72.setText("Probe Height");

        button_updateProbeHeight.setText("Update Eeprom");
        button_updateProbeHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updateProbeHeightActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout setupPanelLayout = new javax.swing.GroupLayout(setupPanel);
        setupPanel.setLayout(setupPanelLayout);
        setupPanelLayout.setHorizontalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setupPanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(setupPanelLayout.createSequentialGroup()
                                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(label_limit_A1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(label_limit_B1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(label_limit_C1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(label_limit_D1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel64, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, setupPanelLayout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(label_limit_B_state, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(label_limit_A_state, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(setupPanelLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(label_limit_C_state, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
                                            .addComponent(label_limit_D_state, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                            .addComponent(button_homeAll1, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(58, 58, 58)
                        .addComponent(button_limitCheck, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE))
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(setupPanelLayout.createSequentialGroup()
                                .addComponent(jLabel68, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(text_setup_probeStartHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(button_updateProbeStartHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(setupPanelLayout.createSequentialGroup()
                                    .addComponent(jLabel65, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(text_setup_maxPrintRadius, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(button_updatePrintRadius, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(setupPanelLayout.createSequentialGroup()
                                    .addComponent(jLabel66, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(text_setup_height, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(button_updateHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(setupPanelLayout.createSequentialGroup()
                                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel70, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel72, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel71, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(setupPanelLayout.createSequentialGroup()
                                        .addComponent(text_setup_probeHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(button_updateProbeHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(setupPanelLayout.createSequentialGroup()
                                        .addComponent(text_setup_rodLength, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(button_updateRodLength, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(setupPanelLayout.createSequentialGroup()
                                        .addComponent(text_setup_rodAtZero, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(button_updateRadiusAtZero, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(setupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(button_startProbeCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(setupPanelLayout.createSequentialGroup()
                            .addComponent(button_Zpos2, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(button_Zneg2, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(text_Nudge_Distance2, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel67, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(18, 18, 18)
                            .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(button_setProbeUsingCurZ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(toggle_allowNegativeOnProbe, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addComponent(jLabel69, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_posX2, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_posY2, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(text_posZ2, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        setupPanelLayout.setVerticalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addComponent(jLabel64)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(label_limit_A1)
                            .addComponent(label_limit_A_state))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(label_limit_B_state)
                            .addComponent(label_limit_B1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(label_limit_C_state)
                            .addComponent(label_limit_C1))
                        .addGap(12, 12, 12)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(label_limit_D1)
                            .addComponent(label_limit_D_state))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(button_homeAll1)
                        .addGap(16, 16, 16))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE)
                    .addComponent(button_limitCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel65)
                    .addComponent(text_setup_maxPrintRadius, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_updatePrintRadius))
                .addGap(6, 6, 6)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel66)
                    .addComponent(text_setup_height, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_updateHeight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel68)
                    .addComponent(text_setup_probeStartHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_updateProbeStartHeight))
                .addGap(4, 4, 4)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel72)
                    .addComponent(text_setup_probeHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_updateProbeHeight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel70)
                    .addComponent(text_setup_rodLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_updateRodLength))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel71)
                    .addComponent(text_setup_rodAtZero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_updateRadiusAtZero))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(button_startProbeCheck)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(button_Zpos2, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(button_Zneg2, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel67, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(toggle_allowNegativeOnProbe))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(text_Nudge_Distance2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(button_setProbeUsingCurZ))))
                .addGap(18, 18, 18)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel69)
                    .addComponent(text_posY2)
                    .addComponent(text_posX2)
                    .addComponent(text_posZ2))
                .addContainerGap(136, Short.MAX_VALUE))
        );

        tabs.addTab("Setup", null, setupPanel, "");

        jLabel11.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.light"));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/easydeltapie/deltaPic.jpg"))); // NOI18N
        jLabel11.setFocusable(false);
        jLabel11.setIconTextGap(0);
        jLabel11.setOpaque(true);

        jScrollPane1.setMaximumSize(new java.awt.Dimension(300, 64));
        jScrollPane1.setMinimumSize(new java.awt.Dimension(300, 64));
        jScrollPane1.setName(""); // NOI18N
        jScrollPane1.setPreferredSize(new java.awt.Dimension(300, 64));

        jTextPane1.setEditable(false);
        jTextPane1.setText("Make sure: \n1.) The height is set close. (See Gantry Tab)\n2.) The probe sensor is working. (See Gantry Tab)\n3.) The head is clean if head is part of probe (No dripping plastic)\nElse the head will crash into the build plate.");
        jTextPane1.setPreferredSize(new java.awt.Dimension(52, 52));
        jScrollPane1.setViewportView(jTextPane1);

        jLabel13.setText("% Max Build Diameter");

        text_level_taps.setText("5");

        jLabel14.setText("Number of Taps");

        jLabel15.setText("Max Coefficent of Variation");

        text_level_cov.setText("0.4");

        jLabel16.setText("Status");

        jLabel17.setText("Press Start to Begin");

        text_level_output.setColumns(20);
        text_level_output.setRows(5);
        scroll_deltaLevel.setViewportView(text_level_output);

        button_autoLevel.setText("Lets Do This (Hand on the Kill Switch)");
        button_autoLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_autoLevelActionPerformed(evt);
            }
        });

        slider_radPer.setMajorTickSpacing(10);
        slider_radPer.setMinimum(50);
        slider_radPer.setPaintLabels(true);
        slider_radPer.setPaintTicks(true);
        slider_radPer.setSnapToTicks(true);
        slider_radPer.setValue(70);

        javax.swing.GroupLayout levelPanelLayout = new javax.swing.GroupLayout(levelPanel);
        levelPanel.setLayout(levelPanelLayout);
        levelPanelLayout.setHorizontalGroup(
            levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(levelPanelLayout.createSequentialGroup()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(button_autoLevel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progress_level, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scroll_deltaLevel)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, levelPanelLayout.createSequentialGroup()
                        .addGroup(levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                            .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(text_level_cov)
                            .addComponent(text_level_taps)
                            .addComponent(slider_radPer, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addContainerGap(44, Short.MAX_VALUE))
        );
        levelPanelLayout.setVerticalGroup(
            levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(levelPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(slider_radPer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(text_level_taps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(levelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(text_level_cov, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(button_autoLevel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress_level, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll_deltaLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 170, Short.MAX_VALUE))
            .addGroup(levelPanelLayout.createSequentialGroup()
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabs.addTab("AutoLevel", levelPanel);

        jScrollPane3.setMaximumSize(new java.awt.Dimension(300, 64));
        jScrollPane3.setMinimumSize(new java.awt.Dimension(300, 64));
        jScrollPane3.setName(""); // NOI18N
        jScrollPane3.setPreferredSize(new java.awt.Dimension(300, 64));

        jTextPane2.setEditable(false);
        jTextPane2.setText("This manual leve tab if for machines with a poor quality probe (or none) that good enough to really dial in extremely good first layer quality.\nMake sure: \n1.) You already ran a AutoLevel if you can. (See AutoLevel Tab)\n2.) You have homed\n3.) Click each position and bring the nozzle to the build plate at the correct height for first layer\n4.) Click the save button to add the position\n5.) Once all positions are saved click calculate to adjust tower height tower offsets and radius.\n6.) Note if only one position  is filled but others are empty it will adjust the tower height only or tower offset.\n7.) You can tweak manualy the tower offsets and convex/concave\n8.) Save early save often... (See Connect Tab)\n");
        jTextPane2.setPreferredSize(new java.awt.Dimension(52, 52));
        jScrollPane3.setViewportView(jTextPane2);

        jLabel21.setText("% Max Build Diameter");

        slider_detailRadPer.setMajorTickSpacing(10);
        slider_detailRadPer.setMinimum(50);
        slider_detailRadPer.setPaintLabels(true);
        slider_detailRadPer.setPaintTicks(true);
        slider_detailRadPer.setSnapToTicks(true);
        slider_detailRadPer.setValue(80);

        slider_detailPoint.setMajorTickSpacing(1);
        slider_detailPoint.setMaximum(4);
        slider_detailPoint.setMinimum(1);
        slider_detailPoint.setPaintLabels(true);
        slider_detailPoint.setPaintTicks(true);
        slider_detailPoint.setSnapToTicks(true);
        slider_detailPoint.setToolTipText("");
        slider_detailPoint.setValue(1);

        jLabel10.setText("Position");

        button_detailSavePoint.setText("Save Point Height");
        button_detailSavePoint.setMaximumSize(new java.awt.Dimension(160, 65));
        button_detailSavePoint.setMinimumSize(new java.awt.Dimension(160, 65));
        button_detailSavePoint.setPreferredSize(new java.awt.Dimension(100, 65));
        button_detailSavePoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detailSavePointActionPerformed(evt);
            }
        });

        button_calcTowerOffsets.setText("Calculate New Parameters");
        button_calcTowerOffsets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_calcTowerOffsetsActionPerformed(evt);
            }
        });

        text_detailHeight.setText("1");
        text_detailHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                text_detailHeightActionPerformed(evt);
            }
        });

        jLabel30.setText("Starting Height");

        button_Zpos1.setText("+Z");
        button_Zpos1.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Zpos1.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Zpos1.setPreferredSize(new java.awt.Dimension(65, 65));
        button_Zpos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_Zpos1ActionPerformed(evt);
            }
        });

        button_Zneg1.setText("-Z");
        button_Zneg1.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Zneg1.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Zneg1.setPreferredSize(new java.awt.Dimension(65, 65));
        button_Zneg1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_Zneg1ActionPerformed(evt);
            }
        });

        text_Nudge_Distance1.setModel(new javax.swing.SpinnerListModel(new String[] {".01", ".03", ".05", ".10", ".30", ".50", "1.0", "3.0"}));

        jLabel19.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.light"));
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setIcon(new javax.swing.ImageIcon(getClass().getResource("/easydeltapie/deltaDetailPic.jpg"))); // NOI18N
        jLabel19.setFocusable(false);
        jLabel19.setIconTextGap(0);
        jLabel19.setOpaque(true);

        button_detailGoto.setText("Goto Position");
        button_detailGoto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detailGotoActionPerformed(evt);
            }
        });

        jLabel59.setText("Position 1");

        text_detail_p1.setText("na");

        text_detail_p2.setText("na");

        jLabel60.setText("Position 2");

        text_detail_p3.setText("na");

        jLabel61.setText("Position 3");

        text_detail_p4.setText("na");

        jLabel62.setText("Position 4");

        jLabel63.setText("Current Pos");

        text_posY1.setText("na");

        text_posX1.setText("na");

        text_posZ1.setText("na");

        toggle_AllowNegHeightDetail.setText("Allow Negative Height");

        button_detail_clearAll.setText("Clear Points");
        button_detail_clearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_clearAllActionPerformed(evt);
            }
        });

        button_detail_Ap.setText("A+");
        button_detail_Ap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_ApActionPerformed(evt);
            }
        });

        button_detail_Am.setText("A-");
        button_detail_Am.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_AmActionPerformed(evt);
            }
        });

        button_detail_Bp.setText("B+");
        button_detail_Bp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_BpActionPerformed(evt);
            }
        });

        button_detail_Bm.setText("B-");
        button_detail_Bm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_BmActionPerformed(evt);
            }
        });

        button_detail_setAllZero.setText("Set All Points to Zero");
        button_detail_setAllZero.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_setAllZeroActionPerformed(evt);
            }
        });

        button_detail_Cp.setText("C+");
        button_detail_Cp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_CpActionPerformed(evt);
            }
        });

        button_detail_Cm.setText("C-");
        button_detail_Cm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_CmActionPerformed(evt);
            }
        });

        button_detail_Rp.setText("R+");
        button_detail_Rp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_RpActionPerformed(evt);
            }
        });

        button_detail_Rm.setText("R-");
        button_detail_Rm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_detail_RmActionPerformed(evt);
            }
        });

        jLabel53.setText("Tweaking Value");

        text_detail_TowerTweak.setText("0.1");
        text_detail_TowerTweak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                text_detail_TowerTweakActionPerformed(evt);
            }
        });

        button_homeAll2.setText("Home All");
        button_homeAll2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_homeAll2ActionPerformed(evt);
            }
        });

        jLabel5.setText("Convex (ABC-)");

        jLabel12.setText("Concave (ABC+)");

        jLabel58.setText("mm (Steps are converted)");

        javax.swing.GroupLayout detailPanelLayout = new javax.swing.GroupLayout(detailPanel);
        detailPanel.setLayout(detailPanelLayout);
        detailPanelLayout.setHorizontalGroup(
            detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailPanelLayout.createSequentialGroup()
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19)
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(button_homeAll2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(detailPanelLayout.createSequentialGroup()
                                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(button_detail_Am, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(button_detail_Ap, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(button_detail_Bm, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(button_detail_Bp, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(button_detail_Cm, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(button_detail_Cp, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(18, 18, 18)
                                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(button_detail_Rm, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(button_detail_Rp, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(button_calcTowerOffsets, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addComponent(jLabel60, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(text_detail_p2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addComponent(jLabel61, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(text_detail_p3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, detailPanelLayout.createSequentialGroup()
                                .addComponent(jLabel62, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(text_detail_p4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(detailPanelLayout.createSequentialGroup()
                                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel21, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(slider_detailRadPer, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(button_detailGoto, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, detailPanelLayout.createSequentialGroup()
                                            .addComponent(jLabel59, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(text_detail_p1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(slider_detailPoint, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(text_Nudge_Distance1, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, detailPanelLayout.createSequentialGroup()
                                            .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(text_detailHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(detailPanelLayout.createSequentialGroup()
                                            .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, detailPanelLayout.createSequentialGroup()
                                                    .addComponent(jLabel63, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(text_posX1, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGroup(detailPanelLayout.createSequentialGroup()
                                                    .addComponent(button_Zneg1, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(button_Zpos1, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(button_detailSavePoint, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                                                .addGroup(detailPanelLayout.createSequentialGroup()
                                                    .addComponent(text_posY1, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(text_posZ1, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addComponent(toggle_AllowNegHeightDetail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                    .addGroup(detailPanelLayout.createSequentialGroup()
                                        .addComponent(button_detail_clearAll, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(button_detail_setAllZero, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 6, Short.MAX_VALUE))))
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addComponent(jLabel53, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_detail_TowerTweak, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel58, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        detailPanelLayout.setVerticalGroup(
            detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailPanelLayout.createSequentialGroup()
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(17, 17, 17)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(slider_detailPoint, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(slider_detailRadPer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel30)
                            .addComponent(text_detailHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_detailGoto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(text_Nudge_Distance1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(button_Zneg1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(detailPanelLayout.createSequentialGroup()
                                    .addComponent(toggle_AllowNegHeightDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(button_detailSavePoint, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(button_Zpos1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(7, 7, 7)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel63)
                            .addComponent(text_posY1)
                            .addComponent(text_posX1)
                            .addComponent(text_posZ1))
                        .addGap(18, 18, 18)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel59)
                            .addComponent(text_detail_p1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel60)
                            .addComponent(text_detail_p2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel61)
                            .addComponent(text_detail_p3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel62)
                            .addComponent(text_detail_p4)))
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_homeAll2)))
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addComponent(button_detail_Ap)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(button_detail_Am))
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addComponent(button_detail_Bp)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(button_detail_Bm))
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addComponent(button_detail_Cp)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(button_detail_Cm))))
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(detailPanelLayout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(button_detail_Rp)
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, detailPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(button_detail_clearAll)
                                    .addComponent(button_detail_setAllZero))
                                .addGap(13, 13, 13)))
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(button_detail_Rm)
                            .addComponent(button_calcTowerOffsets)
                            .addComponent(jLabel12))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel53)
                    .addComponent(text_detail_TowerTweak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel58))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabs.addTab("ManualLevel", detailPanel);

        jLabel24.setText("Z Tolerance Adjustments");

        jLabel25.setText("Machine Properties");

        jLabel26.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel26.setText("Steps per MM");

        text_cal_curStepsMM.setText("a Step");

        jLabel28.setText("User Inputs");

        jLabel32.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel32.setText("Z Calibration Height");

        text_cal_zExpected.setText("35");

        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel33.setText("Z Measured Height");

        button_cal_step.setText("Calculate");
        button_cal_step.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cal_stepActionPerformed(evt);
            }
        });

        jLabel31.setText("Calculated New Step Values for Eeprom");

        jLabel29.setText("Steps per MM");

        text_cal_stepsAdjusted.setText("<NA>");

        button_cal_updateSteps.setText("Update Machine");
        button_cal_updateSteps.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cal_updateStepsActionPerformed(evt);
            }
        });

        jLabel55.setText("Will require re-level");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(button_cal_step, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_cal_curStepsMM, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_cal_zExpected, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_cal_zActual, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(text_cal_stepsAdjusted, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_cal_updateSteps)))
                .addGap(0, 10, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel55, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel24)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel25)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(text_cal_curStepsMM))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel28)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel32)
                    .addComponent(text_cal_zExpected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel33)
                    .addComponent(text_cal_zActual, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(button_cal_step)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel31)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(text_cal_stepsAdjusted)
                    .addComponent(button_cal_updateSteps))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel55)
                .addGap(0, 14, Short.MAX_VALUE))
        );

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel18.setText("Horizontal Radius");

        text_cal_horzRad.setText("aRadius");

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel20.setText("Rod Lenght A");

        jLabel22.setText("Machine Properties");

        jLabel23.setText("X-Y Tolerance Adjustments");

        text_cal_rodA.setText("aRod");

        text_cal_rodB.setText("bRod");

        text_cal_rodC.setText("aRod");

        jLabel34.setText("User Inputs");

        jLabel35.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel35.setText("RCalibration");

        text_cal_radius.setText("60");

        jLabel36.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel36.setText("Ra Measured");

        button_cal_Rods.setText("Calculate");
        button_cal_Rods.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cal_RodsActionPerformed(evt);
            }
        });

        jLabel27.setText("Angle Adjustments");

        jLabel41.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel41.setText("Tower A Angle");

        jLabel43.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel43.setText("Tower B Angle");

        jLabel45.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel45.setText("Tower C Angle");

        button_cal_updateTowerAngle.setText("Update Machine");
        button_cal_updateTowerAngle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cal_updateTowerAngleActionPerformed(evt);
            }
        });

        text_cal_angleC.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        text_cal_angleC.setText("90");

        jLabel47.setText("S/B 210");

        jLabel49.setText("S/B 330");

        jLabel54.setText("S/B 90");

        jLabel57.setText("Will require re-level");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 306, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel41, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(text_cal_angleA, javax.swing.GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel45, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                    .addComponent(jLabel43, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(text_cal_angleB)
                                    .addComponent(text_cal_angleC, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel49, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel47, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel54, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(0, 10, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel57, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(button_cal_updateTowerAngle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel27)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel41)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(text_cal_angleA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel47)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel43)
                    .addComponent(text_cal_angleB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel49))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(text_cal_angleC)
                    .addComponent(jLabel54))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addComponent(button_cal_updateTowerAngle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel57)
                .addGap(20, 20, 20))
        );

        jLabel37.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel37.setText("Rb Measured");

        jLabel38.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel38.setText("Rc Measured");

        jLabel39.setText("Calculated New Rod Values for Eeprom");

        jLabel42.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel42.setText("Rod Lenght B");

        jLabel44.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel44.setText("Rod Lenght C");

        jLabel40.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel40.setText("Rod Lenght A");

        text_cal_calcA.setText("aRod");

        jLabel46.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel46.setText("Rod Lenght B");

        text_cal_calcB.setText("aRod");

        jLabel48.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel48.setText("Rod Lenght C");

        text_cal_calcC.setText("aRod");

        button_cal_updateRods.setText("Update Machine");
        button_cal_updateRods.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cal_updateRodsActionPerformed(evt);
            }
        });

        jLabel56.setText("Will require re-level");

        javax.swing.GroupLayout tolerancePanelLayout = new javax.swing.GroupLayout(tolerancePanel);
        tolerancePanel.setLayout(tolerancePanelLayout);
        tolerancePanelLayout.setHorizontalGroup(
            tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tolerancePanelLayout.createSequentialGroup()
                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tolerancePanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(186, 186, 186))
                    .addGroup(tolerancePanelLayout.createSequentialGroup()
                        .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(tolerancePanelLayout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 735, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(button_cal_Rods, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, tolerancePanelLayout.createSequentialGroup()
                                    .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, tolerancePanelLayout.createSequentialGroup()
                                            .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_horzRad, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(18, 18, 18)
                                            .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_rodA, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, tolerancePanelLayout.createSequentialGroup()
                                            .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_radius, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_radiusA, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(tolerancePanelLayout.createSequentialGroup()
                                            .addComponent(jLabel37, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_radiusB, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(tolerancePanelLayout.createSequentialGroup()
                                            .addComponent(jLabel42, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_rodB, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(tolerancePanelLayout.createSequentialGroup()
                                            .addComponent(jLabel38, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_radiusC, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(tolerancePanelLayout.createSequentialGroup()
                                            .addComponent(jLabel44, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(text_cal_rodC, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addGroup(tolerancePanelLayout.createSequentialGroup()
                                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(tolerancePanelLayout.createSequentialGroup()
                                        .addComponent(jLabel40, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_cal_calcA, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel46, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_cal_calcB, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel48, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_cal_calcC, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel39, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel56, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(button_cal_updateRods))))
                        .addGap(0, 37, Short.MAX_VALUE)))
                .addContainerGap())
        );
        tolerancePanelLayout.setVerticalGroup(
            tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tolerancePanelLayout.createSequentialGroup()
                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(text_cal_rodA)
                    .addComponent(jLabel18)
                    .addComponent(text_cal_horzRad)
                    .addComponent(jLabel42)
                    .addComponent(text_cal_rodB)
                    .addComponent(jLabel44)
                    .addComponent(text_cal_rodC))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel34)
                .addGap(2, 2, 2)
                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel35)
                    .addComponent(text_cal_radius, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel36)
                    .addComponent(text_cal_radiusA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel37)
                    .addComponent(text_cal_radiusB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel38)
                    .addComponent(text_cal_radiusC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(button_cal_Rods)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel39)
                    .addComponent(jLabel56))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(tolerancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel40)
                    .addComponent(text_cal_calcA)
                    .addComponent(jLabel46)
                    .addComponent(text_cal_calcB)
                    .addComponent(jLabel48)
                    .addComponent(text_cal_calcC)
                    .addComponent(button_cal_updateRods))
                .addContainerGap(208, Short.MAX_VALUE))
        );

        tabs.addTab("Tolerance", tolerancePanel);

        gantryPanel.setMaximumSize(new java.awt.Dimension(800, 600));
        gantryPanel.setMinimumSize(new java.awt.Dimension(800, 600));
        gantryPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        motionPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        button_Ypos.setText("+Y");
        button_Ypos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_YposActionPerformed(evt);
            }
        });

        button_Center.setText("Center");
        button_Center.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Center.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Center.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_CenterActionPerformed(evt);
            }
        });

        button_Xneg.setText("-X");
        button_Xneg.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Xneg.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Xneg.setPreferredSize(new java.awt.Dimension(65, 65));
        button_Xneg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_XnegActionPerformed(evt);
            }
        });

        button_Xpos.setText("+X");
        button_Xpos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_XposActionPerformed(evt);
            }
        });

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Step Size mm");
        jLabel6.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        button_Zpos.setText("+Z");
        button_Zpos.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Zpos.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Zpos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_ZposActionPerformed(evt);
            }
        });

        button_Zneg.setText("-Z");
        button_Zneg.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Zneg.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Zneg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_ZnegActionPerformed(evt);
            }
        });

        button_Yneg.setText("-Y");
        button_Yneg.setMaximumSize(new java.awt.Dimension(65, 65));
        button_Yneg.setMinimumSize(new java.awt.Dimension(65, 65));
        button_Yneg.setPreferredSize(new java.awt.Dimension(65, 65));
        button_Yneg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_YnegActionPerformed(evt);
            }
        });

        text_Nudge_Distance.setModel(new javax.swing.SpinnerListModel(new String[] {".01", ".03", ".05", ".10", ".30", ".50", "1.0", "3.0", "5.0", "10.0", "30.0", "50.0", "100.0"}));

        jLabel3.setText("Current Position");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("X:");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Y:");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Z:");

        jLabel51.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel51.setText("R:");

        jLabel52.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel52.setText("T:");

        button_GoToCart.setText("Go To");
        button_GoToCart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_GoToCartActionPerformed(evt);
            }
        });

        button_GoToRad.setText("Go To");
        button_GoToRad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_GoToRadActionPerformed(evt);
            }
        });

        button_Rneg.setText("-R");
        button_Rneg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_RnegActionPerformed(evt);
            }
        });

        button_Rpos.setText("+R");
        button_Rpos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_RposActionPerformed(evt);
            }
        });

        button_Tpos.setText("+T");
        button_Tpos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_TposActionPerformed(evt);
            }
        });

        button_Tneg.setText("-T");
        button_Tneg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_TnegActionPerformed(evt);
            }
        });

        button_homeAll.setText("Home All");
        button_homeAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_homeAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout motionPanelLayout = new javax.swing.GroupLayout(motionPanel);
        motionPanel.setLayout(motionPanelLayout);
        motionPanelLayout.setHorizontalGroup(
            motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(motionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(button_homeAll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(motionPanelLayout.createSequentialGroup()
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(button_Ypos, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(motionPanelLayout.createSequentialGroup()
                                    .addComponent(button_Xneg, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(button_Center, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(button_Yneg, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(motionPanelLayout.createSequentialGroup()
                                .addComponent(button_Xpos, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(23, 23, 23)
                                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addComponent(button_Zneg, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(button_Rneg, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(button_Tneg, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addComponent(button_Zpos, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(button_Rpos, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(button_Tpos, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(motionPanelLayout.createSequentialGroup()
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(text_Nudge_Distance)))))
                .addGap(25, 25, 25)
                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, motionPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(103, 103, 103))
                    .addGroup(motionPanelLayout.createSequentialGroup()
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(motionPanelLayout.createSequentialGroup()
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(text_posZ, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(motionPanelLayout.createSequentialGroup()
                                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_posX, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_posY, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(button_GoToCart, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(46, 46, 46)
                                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addComponent(jLabel51, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_posR, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addComponent(jLabel52, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(text_posT, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(button_GoToRad, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(11, 11, 11))))
        );
        motionPanelLayout.setVerticalGroup(
            motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(motionPanelLayout.createSequentialGroup()
                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(motionPanelLayout.createSequentialGroup()
                        .addComponent(button_homeAll)
                        .addGap(8, 8, 8)
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(button_Ypos, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(button_Zpos, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(button_Rpos, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(button_Tpos, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(motionPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(button_Center, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(button_Xneg, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(button_Xpos, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(button_Rneg, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(button_Tneg, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(button_Zneg, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(button_Yneg, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(motionPanelLayout.createSequentialGroup()
                                        .addGap(32, 32, 32)
                                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(text_Nudge_Distance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addGroup(motionPanelLayout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addComponent(button_GoToRad))))
                    .addGroup(motionPanelLayout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(text_posX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel51)
                            .addComponent(text_posR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(text_posY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel52)
                            .addComponent(text_posT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(motionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(text_posZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(button_GoToCart)))
                .addContainerGap(60, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout gantryPanelLayout = new javax.swing.GroupLayout(gantryPanel);
        gantryPanel.setLayout(gantryPanelLayout);
        gantryPanelLayout.setHorizontalGroup(
            gantryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, gantryPanelLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(motionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 783, Short.MAX_VALUE)
                .addContainerGap())
        );
        gantryPanelLayout.setVerticalGroup(
            gantryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gantryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(motionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(311, Short.MAX_VALUE))
        );

        tabs.addTab("Gantry", null, gantryPanel, "");

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(tabs, javax.swing.GroupLayout.PREFERRED_SIZE, 650, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 50, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_rescanComsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_rescanComsActionPerformed
        java.awt.EventQueue.invokeLater(new PortScanner(this));       
    }//GEN-LAST:event_button_rescanComsActionPerformed

    private void button_ConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_ConnectActionPerformed
        // Should be quick enougth to run on the GUI thread
        DeltaComPort dcp = getSelectedComPort();
        if(dcp == null){
            return;
        }
        // have a com port
        try{
            if(button_Connect.isSelected()){
                // want to connect
                // lock down views
                // start connection
                Thread portThread = new Thread(dcp);
                portThread.start();
            }else{
                // want to disconnect
                dcp.close();
                updatedConnectedViews(false);
            }
        }catch(Exception e){
            button_Connect.setSelected(false);
            updatedConnectedViews(false);
            JOptionPane.showConfirmDialog(this, "Error Changing Connection State");
        }
        
    }//GEN-LAST:event_button_ConnectActionPerformed

    private void button_commandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_commandActionPerformed
        // Check to see if can send then send either way clear the command
        String command = this.text_command.getText().trim();
        this.text_command.setText("");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), command);
        new Thread(sci).start();
        
    }//GEN-LAST:event_button_commandActionPerformed

    private void combo_eepromActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combo_eepromActionPerformed
        // TODO add your handling code here:
        try{
            this.text_eeprom.setText(((EepromValue)this.combo_eeprom.getSelectedItem()).getValue());
        }catch(Exception e){
            this.text_eeprom.setText("");
        }
    }//GEN-LAST:event_combo_eepromActionPerformed

    private void button_eepromActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_eepromActionPerformed
        // TODO add your handling code here:
        try{
            String toSet = this.text_eeprom.getText().trim();
            EepromValue ev = (EepromValue)this.combo_eeprom.getSelectedItem();
            ArrayList<String> commands = new ArrayList<>();
            if(ev.checkValue(toSet)){
                commands.add(this.getEepromState().getEepromWriteCommand(ev, toSet));
                commands.add(getEepromState().getEepromCommand());
                SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
                new Thread(sci).start();
            }else{
                JOptionPane.showMessageDialog(this, "Value not parseable.", "Error Setting Value", JOptionPane.ERROR_MESSAGE);
            }
        }catch(HeadlessException e){
            JOptionPane.showMessageDialog(this, e.toString(), "Error Setting Value", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_button_eepromActionPerformed

    private void button_homeAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_homeAllActionPerformed
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), "G28");
        new Thread(sci).start();

        
    }//GEN-LAST:event_button_homeAllActionPerformed

    private void button_autoLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_autoLevelActionPerformed
        // TODO add your handling code here:
        this.text_level_output.setText("");
        this.summary_level = new StringBuilder();
        this.progress_level.setValue(0);
        int radiusPer = this.slider_radPer.getValue();
        int numTaps = -1;
        double cov = -1.0;
        try{
            numTaps = Integer.parseInt(this.text_level_taps.getText().trim());
        }catch(NumberFormatException e){}
        try{
            cov = Double.parseDouble(this.text_level_cov.getText().trim());
        }catch(NumberFormatException e){}
        if(cov < 0.0 || numTaps < 1){
            // error
        }else{
            Thread aThread = new Thread(new AutoLevelControl(this.getSelectedComPort(),this,radiusPer,numTaps,cov));
            aThread.start();
        }
    }//GEN-LAST:event_button_autoLevelActionPerformed

    private void button_GoToRadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_GoToRadActionPerformed
        // TODO add your handling code here:
        float x = Float.NaN;
        float y = Float.NaN;
        float z = Float.NaN;
        try{
            z = Float.parseFloat(this.text_posZ.getText().trim());
            float r = Float.parseFloat(this.text_posR.getText().trim());
            float t = Float.parseFloat(this.text_posT.getText().trim());
            x = (float)(r*Math.cos(t*Math.PI/180.0));
            y = (float)(r*Math.sin(t*Math.PI/180.0));
        }catch(NumberFormatException e){}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_GoToRadActionPerformed

    private void button_GoToCartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_GoToCartActionPerformed
        // TODO add your handling code here:
        float x = Float.NaN;
        float y = Float.NaN;
        float z = Float.NaN;
        try{
            x = Float.parseFloat(this.text_posX.getText().trim());
            y = Float.parseFloat(this.text_posY.getText().trim());
            z = Float.parseFloat(this.text_posZ.getText().trim());
        }catch(NumberFormatException e){}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();                                              
    }//GEN-LAST:event_button_GoToCartActionPerformed

    private void button_YposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_YposActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 Y"+this.text_Nudge_Distance.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_YposActionPerformed

    private void button_XnegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_XnegActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 X-"+this.text_Nudge_Distance.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_XnegActionPerformed

    private void button_XposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_XposActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 X"+this.text_Nudge_Distance.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_XposActionPerformed

    private void button_YnegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_YnegActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 Y-"+this.text_Nudge_Distance.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_YnegActionPerformed

    private void button_CenterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_CenterActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        commands.add("G0 X0.0 Y0.0");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_CenterActionPerformed

    private void button_ZposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_ZposActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 Z"+this.text_Nudge_Distance.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_ZposActionPerformed

    private void button_ZnegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_ZnegActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 Z-"+this.text_Nudge_Distance.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_ZnegActionPerformed

    private void button_RnegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_RnegActionPerformed
        // TODO add your handling code here:
        // TODO add your handling code here:
        float x = Float.NaN;
        float y = Float.NaN;
        float z = Float.NaN;
        try{
            z = Float.parseFloat(this.text_posZ.getText().trim());
            float r = Float.parseFloat(this.text_posR.getText().trim())-
                    Float.parseFloat(this.text_Nudge_Distance.getValue().toString());
            float t = Float.parseFloat(this.text_posT.getText().trim());
            x = (float)(r*Math.cos(t*Math.PI/180.0));
            y = (float)(r*Math.sin(t*Math.PI/180.0));
        }catch(NumberFormatException e){}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
  
    }//GEN-LAST:event_button_RnegActionPerformed

    private void button_RposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_RposActionPerformed
        // TODO add your handling code here:
        // TODO add your handling code here:
        float x = Float.NaN;
        float y = Float.NaN;
        float z = Float.NaN;
        try{
            z = Float.parseFloat(this.text_posZ.getText().trim());
            float r = Float.parseFloat(this.text_posR.getText().trim())+
                    Float.parseFloat(this.text_Nudge_Distance.getValue().toString());
            float t = Float.parseFloat(this.text_posT.getText().trim());
            x = (float)(r*Math.cos(t*Math.PI/180.0));
            y = (float)(r*Math.sin(t*Math.PI/180.0));
        }catch(NumberFormatException e){}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
        
    }//GEN-LAST:event_button_RposActionPerformed

    private void button_TposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_TposActionPerformed
        float x = Float.NaN;
        float y = Float.NaN;
        float z = Float.NaN;
        try{
            z = Float.parseFloat(this.text_posZ.getText().trim());
            float r = Float.parseFloat(this.text_posR.getText().trim());
            float t = Float.parseFloat(this.text_posT.getText().trim())+
                    Float.parseFloat(this.text_Nudge_Distance.getValue().toString());
            x = (float)(r*Math.cos(t*Math.PI/180.0));
            y = (float)(r*Math.sin(t*Math.PI/180.0));
        }catch(NumberFormatException e){}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
      }//GEN-LAST:event_button_TposActionPerformed

    private void button_TnegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_TnegActionPerformed
        float x = Float.NaN;
        float y = Float.NaN;
        float z = Float.NaN;
        try{
            z = Float.parseFloat(this.text_posZ.getText().trim());
            float r = Float.parseFloat(this.text_posR.getText().trim());
            float t = Float.parseFloat(this.text_posT.getText().trim())-
                    Float.parseFloat(this.text_Nudge_Distance.getValue().toString());
            x = (float)(r*Math.cos(t*Math.PI/180.0));
            y = (float)(r*Math.sin(t*Math.PI/180.0));
        }catch(NumberFormatException e){}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_TnegActionPerformed

    private void button_cal_stepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cal_stepActionPerformed
        // TODO add your handling code here:
        float currentStep = Float.NaN;
        float sbHeight = Float.NaN;
        float isHeight = Float.NaN;
        try{
            currentStep = this.getSelectedComPort().getFirmwareParser().getEepromState().getStepsPerMM().getValueAsFloat();
            sbHeight = Float.parseFloat(this.text_cal_zExpected.getText().trim());
            isHeight = Float.parseFloat(this.text_cal_zActual.getText().trim());
        }catch (NumberFormatException e){}
        if(!Float.isNaN(sbHeight) && !Float.isNaN(isHeight)&& !Float.isNaN(currentStep)){
            float newStep = sbHeight/isHeight*currentStep;
            this.text_cal_stepsAdjusted.setText(""+newStep);
        }else{
            this.text_cal_stepsAdjusted.setText("");
        }
    }//GEN-LAST:event_button_cal_stepActionPerformed

    private void button_cal_updateStepsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cal_updateStepsActionPerformed
        // TODO add your handling code here:
        float newStep = Float.NaN;
        try{
            newStep = Float.parseFloat(this.text_cal_stepsAdjusted.getText().trim());
        }catch (NumberFormatException e){}
        if(!Float.isNaN(newStep)){
            EepromValue step = this.getSelectedComPort().getFirmwareParser().getEepromState().getStepsPerMM();
            ArrayList<String> commands = new ArrayList<String>();
            commands.add(getEepromState().getEepromWriteCommand(step, ""+(float)newStep));
            commands.add(getEepromState().getEepromCommand());
            SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
            new Thread(sci).start();
        }        
    }//GEN-LAST:event_button_cal_updateStepsActionPerformed

    private void button_cal_RodsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cal_RodsActionPerformed
        // Get user variables
        float la = -1.0f;
        float lb = -1.0f;
        float lc = -1.0f;
        float lx = -1.0f;
        try {
            la = Float.parseFloat(this.text_cal_radiusA.getText().trim());
            lb = Float.parseFloat(this.text_cal_radiusB.getText().trim());
            lc = Float.parseFloat(this.text_cal_radiusC.getText().trim());
            lx = Float.parseFloat(this.text_cal_radius.getText().trim());
        } catch (NumberFormatException numberFormatException) {
            JOptionPane.showMessageDialog(this, "ERROR with Raidal inputs they must be number...  Are you trying to mess up your printer?");
            return;
        }
        if((la < 0.0) || (lb < 0.0) || (lc < 0.0) || (lx < 0.0)){
            JOptionPane.showMessageDialog(this, "Values must be posative...  Are you trying to mess up your printer?");
            return;
        }
        float ls[] = {la, lb, lc};
        float rods[] = this.getSelectedComPort().getFirmwareParser().getEepromState().getDiagonalRodLenghts();
        float radius = this.getSelectedComPort().getFirmwareParser().getEepromState().getHorizontalRodRadius().getValueAsFloat();
        float rodsP[] = new float[3];
        // have everything I need.
        for(int i=0; i<3; i++){
            rodsP[i] = (float)(RodLengthCalc.getNewRodLenght(ls[i],rods[i],radius,lx));
        }
        this.text_cal_calcA.setText(""+rodsP[0]);
        this.text_cal_calcB.setText(""+rodsP[1]);
        this.text_cal_calcC.setText(""+rodsP[2]);

    }//GEN-LAST:event_button_cal_RodsActionPerformed

    private void button_cal_updateTowerAngleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cal_updateTowerAngleActionPerformed
        // TODO add your handling code here:
        float angleA = Float.NaN;
        float angleB = Float.NaN;
        try{
            angleA = Float.parseFloat(this.text_cal_angleA.getText().trim());
            angleB = Float.parseFloat(this.text_cal_angleB.getText().trim());
            if(angleA > 220 || angleA<200){
                angleA = Float.NaN;
            }
            if(angleB > 340 || angleB < 320){
                angleB = Float.NaN;
            }
        }catch (NumberFormatException e){}
        if(!Float.isNaN(angleA) && !Float.isNaN(angleB)){
            ArrayList<String> commands = new ArrayList<String>();
            commands.addAll(this.getEepromState().getTowerAngleAdjustEepromCommand(angleA, angleB, 90.0f));
            commands.add(getEepromState().getEepromCommand());
            SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
            new Thread(sci).start();
            
        }
    }//GEN-LAST:event_button_cal_updateTowerAngleActionPerformed

    private void button_cal_updateRodsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cal_updateRodsActionPerformed
        // TODO add your handling code here:
        float rodsP[] = new float[3];
        try{
            rodsP[0] = Float.parseFloat(this.text_cal_calcA.getText());
            rodsP[1] = Float.parseFloat(this.text_cal_calcB.getText());
            rodsP[2] = Float.parseFloat(this.text_cal_calcC.getText());
        }catch(Exception e){
            JOptionPane.showMessageDialog(this, "ERROR with Raidal inputs Did you press calculate first and check the values?");
            return;            
        }
        ArrayList<String> commands = new ArrayList<String>();
        commands.addAll(this.getSelectedComPort().getFirmwareParser().getEepromState().getDiagonalRodLenghtsEepromCommands(rodsP));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();        
    }//GEN-LAST:event_button_cal_updateRodsActionPerformed

    private void button_limitCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_limitCheckActionPerformed
        DeltaComPort dcp = getSelectedComPort();
        if(dcp == null){
            return;
        }
        // have a com port
        try{
            if(this.button_limitCheck.isSelected()){
                // want to connect
                // lock down views
                // start connection
                immuneToEnable.add(this.button_limitCheck);
                limitChecker = new LimitCheckController(this.getSelectedComPort(), this);
                Thread limitThread = new Thread(limitChecker);
                limitThread.start();
            }else{
                // want to disconnect
                limitChecker.selfdestruct();
                immuneToEnable.clear();
                colorLimitLabel(this.label_limit_A_state, false);
                colorLimitLabel(this.label_limit_B_state, false);
                colorLimitLabel(this.label_limit_C_state, false);
                colorLimitLabel(this.label_limit_D_state, false);
            }
        }catch(Exception e){
            this.button_limitCheck.setSelected(false);
            resetConnection();
        }

    }//GEN-LAST:event_button_limitCheckActionPerformed

    private void button_updatePrintRadiusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updatePrintRadiusActionPerformed
        // TODO add your handling code here:
        double val = -1.0;
        EepromValue dia = this.getSelectedComPort().getFirmwareParser().getEepromState().getMaxBuildRadius();
        try{val = Double.parseDouble(this.text_setup_maxPrintRadius.getText().trim());}catch(NumberFormatException e){}
        if(val < 0.0){
            this.text_setup_maxPrintRadius.setText(dia.getValue());
            return;
        }
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(getEepromState().getEepromWriteCommand(dia, ""+(float)val));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_updatePrintRadiusActionPerformed

    private void button_updateHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updateHeightActionPerformed
        // TODO add your handling code here:
        float val = Float.NaN;
        EepromValue height = this.getSelectedComPort().getFirmwareParser().getEepromState().getHomedHeight();
        try{val = Float.parseFloat(this.text_setup_height.getText().trim());}catch(NumberFormatException e){}
        if(Float.isNaN(val) || val < 0.0){
            this.text_setup_height.setText(height.getValue());
            return;
        }
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(getEepromState().getEepromWriteCommand(height, ""+(float)val));
        commands.add(getEepromState().getEepromCommand());
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_updateHeightActionPerformed

    private void button_Zpos2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_Zpos2ActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 Z"+this.text_Nudge_Distance2.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_Zpos2ActionPerformed

    private void button_Zneg2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_Zneg2ActionPerformed
        // add negative check and allowed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        if(this.toggle_allowNegativeOnProbe.isSelected()){
            // ignore boundary
            commands.add("G0 S1");
        }
        commands.add("G0 Z-"+this.text_Nudge_Distance2.getValue().toString());
        if(this.toggle_allowNegativeOnProbe.isSelected()){
            // use boundary
            commands.add("G0 S0");
        }
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
        // TODO add your handling code here:
    }//GEN-LAST:event_button_Zneg2ActionPerformed

    private void button_setProbeUsingCurZActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_setProbeUsingCurZActionPerformed
        float val = Float.NaN;
        EepromValue probe = this.getSelectedComPort().getFirmwareParser().getEepromState().getProbeZHeight();
        try{val = Float.parseFloat(this.text_posZ2.getText().trim());}catch(NumberFormatException e){}
        if(Float.isNaN(val)){return;}
        float nProbHeight = probe.getValueAsFloat()-val;
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(getEepromState().getEepromWriteCommand(probe, ""+(float)nProbHeight));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
        
        
    }//GEN-LAST:event_button_setProbeUsingCurZActionPerformed

    private void button_homeAll1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_homeAll1ActionPerformed
        // TODO add your handling code here:
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), "G28");
        new Thread(sci).start();

    }//GEN-LAST:event_button_homeAll1ActionPerformed

    private void toggle_allowNegativeOnProbeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggle_allowNegativeOnProbeActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_toggle_allowNegativeOnProbeActionPerformed

    private void button_updateProbeStartHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updateProbeStartHeightActionPerformed
        float val = Float.NaN;
        EepromValue probe = this.getSelectedComPort().getFirmwareParser().getEepromState().getProbeZStartHeight();
        try{val = Float.parseFloat(this.text_setup_probeStartHeight.getText().trim());}catch(NumberFormatException e){}
        if(Float.isNaN(val)){return;}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(getEepromState().getEepromWriteCommand(probe, ""+(float)val));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_updateProbeStartHeightActionPerformed

    private void button_startProbeCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_startProbeCheckActionPerformed
        // setup new Single Probe control
        Thread aThread = new Thread(new SingleProbeControl(this.getSelectedComPort(), 3));
        aThread.start();        
    }//GEN-LAST:event_button_startProbeCheckActionPerformed

    private void button_updateRodLengthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updateRodLengthActionPerformed
        float val = Float.NaN;
        EepromValue rod = this.getSelectedComPort().getFirmwareParser().getEepromState().getDiagonalRodLength();
        try{val = Float.parseFloat(this.text_setup_probeStartHeight.getText().trim());}catch(NumberFormatException e){}
        if(Float.isNaN(val)){return;}
        float rodsP[] = new float[3];
        rodsP[0] = val;
        rodsP[1] = val;
        rodsP[2] = val;
        ArrayList<String> commands = new ArrayList<String>();
        commands.addAll(this.getSelectedComPort().getFirmwareParser().getEepromState().getDiagonalRodLenghtsEepromCommands(rodsP));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_updateRodLengthActionPerformed

    private void button_updateRadiusAtZeroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updateRadiusAtZeroActionPerformed
        float val = Float.NaN;
        EepromValue probe = this.getSelectedComPort().getFirmwareParser().getEepromState().getHorizontalRodRadius();
        try{val = Float.parseFloat(this.text_setup_rodAtZero.getText().trim());}catch(NumberFormatException e){}
        if(Float.isNaN(val)){return;}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(getEepromState().getEepromWriteCommand(probe, ""+(float)val));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_updateRadiusAtZeroActionPerformed

    private void button_updateProbeHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updateProbeHeightActionPerformed
        float val = Float.NaN;
        EepromValue probe = this.getSelectedComPort().getFirmwareParser().getEepromState().getProbeZHeight();
        try{val = Float.parseFloat(this.text_setup_probeHeight.getText().trim());}catch(NumberFormatException e){}
        if(Float.isNaN(val)){return;}
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(getEepromState().getEepromWriteCommand(probe, ""+(float)val));
        commands.add(getEepromState().getEepromCommand());
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_updateProbeHeightActionPerformed

    private void button_writeOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_writeOutActionPerformed
        File out = null;
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
        "Tab Delimit Files", "txt");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           out = chooser.getSelectedFile();
        }
        try{
            PrintWriter pw = new PrintWriter(out);
            this.getEepromState().printOut(pw);
            pw.flush();
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EasyDeltaPie.class.getName()).log(Level.SEVERE, null, ex);
        }

        
    }//GEN-LAST:event_button_writeOutActionPerformed

    private void button_homeAll2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_homeAll2ActionPerformed
        // TODO add your handling code here:
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), "G28");
        new Thread(sci).start();
    }//GEN-LAST:event_button_homeAll2ActionPerformed

    private void text_detail_TowerTweakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_text_detail_TowerTweakActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_text_detail_TowerTweakActionPerformed

    private void button_detail_setAllZeroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_setAllZeroActionPerformed
        // TODO add your handling code here:
        int perRadius = this.slider_detailRadPer.getValue();
        double r = getSelectedComPort().getFirmwareParser().getEepromState().getMaxBuildRadius().getValueAsFloat()*perRadius/100.0;
        int point = this.slider_detailPoint.getValue();
        float x = 0.0f;
        float y = 0.0f;
        float z = 0.0f;
        // point 1 just keep at zero
        JLabel j = text_detail_p1;
        j.setText(x+","+y+","+z);

        // point == 2
        // Tower A at 210
        x = (float)(r*Math.cos(210.0*Math.PI/180.0));
        y = (float)(r*Math.sin(210.0*Math.PI/180.0));
        j = text_detail_p2;
        j.setText(x+","+y+","+z);
        //point == 3
        // Tower B at 330
        x = (float)(r*Math.cos(330.0*Math.PI/180.0));
        y = (float)(r*Math.sin(330.0*Math.PI/180.0));
        j = text_detail_p3;
        j.setText(x+","+y+","+z);
        //point == 4
        // Tower C at 90
        x = (float)(r*Math.cos(90.0*Math.PI/180.0));
        y = (float)(r*Math.sin(90.0*Math.PI/180.0));
        j = text_detail_p4;
        j.setText(x+","+y+","+z);
    }//GEN-LAST:event_button_detail_setAllZeroActionPerformed

    private void button_detail_clearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_clearAllActionPerformed
        text_detail_p1.setText("na");
        text_detail_p2.setText("na");
        text_detail_p3.setText("na");
        text_detail_p4.setText("na");
    }//GEN-LAST:event_button_detail_clearAllActionPerformed

    private void button_detailGotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detailGotoActionPerformed
        // TODO add your handling code here:
        moveToDetailPoint();
    }//GEN-LAST:event_button_detailGotoActionPerformed

    private void button_Zneg1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_Zneg1ActionPerformed
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        if(this.toggle_AllowNegHeightDetail.isSelected()){
            // ignore boundary
            commands.add("G0 S1");
        }
        commands.add("G0 Z-"+this.text_Nudge_Distance1.getValue().toString());
        if(this.toggle_AllowNegHeightDetail.isSelected()){
            // ignore boundary
            commands.add("G0 S0");
        }
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
        // TODO add your handling code here:
    }//GEN-LAST:event_button_Zneg1ActionPerformed

    private void button_Zpos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_Zpos1ActionPerformed
        // TODO add your handling code here:
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G91");
        commands.add("G0 Z"+this.text_Nudge_Distance1.getValue().toString());
        commands.add("G90");
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_Zpos1ActionPerformed

    private void text_detailHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_text_detailHeightActionPerformed
        // TODO add your handling code here:
        float height = 0f;
        try {height = Float.parseFloat(this.text_detailHeight.getText().trim());} catch (NumberFormatException numberFormatException) {}
        if (height < 0){
            this.text_detailHeight.setText(""+height);
        }
    }//GEN-LAST:event_text_detailHeightActionPerformed

    private void button_calcTowerOffsetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_calcTowerOffsetsActionPerformed
        // TODO add your handling code here:
        EepromValue height = this.getSelectedComPort().getFirmwareParser().getEepromState().getHomedHeight();
        float horizRad = this.getSelectedComPort().getFirmwareParser().getEepromState().getHorizontalRodRadius().getValueAsFloat();
        // get coordinates
        BedProbePoint p1 = getCoordFromLabel(text_detail_p1);
        BedProbePoint p2 = getCoordFromLabel(text_detail_p2);
        BedProbePoint p3 = getCoordFromLabel(text_detail_p3);
        BedProbePoint p4 = getCoordFromLabel(text_detail_p4);
        if(p1 != null){
            if (p2 != null && p3 != null && p4!=null){
                // use the echer setup
                ArrayList<BedProbePoint> points = new ArrayList<>();
                points.add(p1);
                points.add(p2);
                points.add(p3);
                points.add(p4);
                EepromState es = this.getEepromState();
                DeltaParameters params;
                try {
                    params = es.getParametersForEscher3D();
                    DeltaParameters startParam = params.makeClone();
                    DeltaCalibration calibrate = new DeltaCalibration(params, points, true);
                    calibrate.doDeltaCalibration(4);
                    DeltaParameters finalParam = calibrate.getParameters();
                    double[] endsN = finalParam.getStopAdjusts();
                    double[] endsO = startParam.getStopAdjusts();
                    StringBuilder dal = new StringBuilder();
                    dal.append("Rad @ 0.0\t"+f(startParam.getHorizontalRodRadius())+"\t"+f(finalParam.getHorizontalRodRadius()));
                    dal.append("\n");
                    dal.append("Height\t"+f(startParam.getHomedHeight())+"\t"+f(finalParam.getHomedHeight()));
                    dal.append("\n");
                    dal.append("Aadjust\t"+f(endsO[0])+"\t"+f(endsN[0]));
                    dal.append("\n");
                    dal.append("Badjust\t"+f(endsO[1])+"\t"+f(endsN[1]));
                    dal.append("\n");
                    dal.append("Cadjust\t"+f(endsO[2])+"\t"+f(endsN[2]));
                    dal.append("\n");
                    int opt = JOptionPane.showConfirmDialog(this,dal.toString(), "Update Firmware?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if(opt == JOptionPane.YES_OPTION){
                        ArrayList<String> commands = finalParam.setCommandsForEscher3D(es);
                        commands.add(es.getEepromCommand());
                        commands.add("G28");
                        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
                        new Thread(sci).start();
                    }
                } catch (CalibrationException ex) {
                    Logger.getLogger(EasyDeltaPie.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                // just offset height
                float val = height.getValueAsFloat() - p1.z();
                ArrayList<String> commands = new ArrayList<String>();
                commands.add(getEepromState().getEepromWriteCommand(height, ""+val));
                commands.add(getEepromState().getEepromCommand());
                commands.add("G28");
                SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
                new Thread(sci).start();
            }
        }else if(p1 == null && p2 != null && p3 == null && p4 == null){
            // p2 adjust A height
            float rad = (float)Math.sqrt(p2.x()*p2.x()+p2.y()*p2.y());
            float dist = p2.z();
            float factor = horizRad/rad;
            float[] towers = this.getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();
            towers[0] = towers[0] + dist*factor;
            ArrayList<String> commands = new ArrayList<String>();
            commands.addAll(getEepromState().getTowerStopAdjustEepromCommands(towers));
            commands.add(getEepromState().getEepromCommand());
            commands.add("G28");
            SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
            new Thread(sci).start();
        }else if(p1 == null && p2 == null && p3 != null && p4 == null){
            // p3 adjust B height
            float rad = (float)Math.sqrt(p3.x()*p3.x()+p3.y()*p3.y());
            float dist = p3.z();
            float factor = horizRad/rad;
            float[] towers = this.getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();
            towers[1] = towers[1] + dist*factor;
            ArrayList<String> commands = new ArrayList<String>();
            commands.addAll(getEepromState().getTowerStopAdjustEepromCommands(towers));
            commands.add(getEepromState().getEepromCommand());
            commands.add("G28");
            SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
            new Thread(sci).start();
            
        }else if(p1 == null && p2 == null && p3 == null && p4 != null){
            // p4 adjust C height
            float rad = (float)Math.sqrt(p4.x()*p4.x()+p4.y()*p4.y());
            float dist = p4.z();
            float factor = horizRad/rad;
            float[] towers = this.getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();
            towers[2] = towers[2] + dist*factor;
            ArrayList<String> commands = new ArrayList<String>();
            commands.addAll(getEepromState().getTowerStopAdjustEepromCommands(towers));
            commands.add(getEepromState().getEepromCommand());
            commands.add("G28");
            SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
            new Thread(sci).start();
        }

    }//GEN-LAST:event_button_calcTowerOffsetsActionPerformed

    private void button_detailSavePointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detailSavePointActionPerformed
        // TODO add your handling code here:
        // check that its a position
        int perRadius = this.slider_detailRadPer.getValue();
        double r = getSelectedComPort().getFirmwareParser().getEepromState().getMaxBuildRadius().getValueAsFloat()*perRadius/100.0;
        int point = this.slider_detailPoint.getValue();
        float x = 0.0f;
        float y = 0.0f;
        float za = Float.NaN;
        float xa = Float.NaN;
        float ya = Float.NaN;
        JLabel j = text_detail_p1;
        try{
            xa = Float.parseFloat(this.text_posX1.getText().trim());
            ya = Float.parseFloat(this.text_posY1.getText().trim());
            za = Float.parseFloat(this.text_posZ1.getText().trim());
        }catch(NumberFormatException e){}
        if(Float.isNaN(xa) || Float.isNaN(ya) || Float.isNaN(za)){
            // throw an error? should be error trapped at set
            return;
        }
        // point 1 just keep at zero
        if(point == 2){
            // Tower A at 210
            x = (float)(r*Math.cos(210.0*Math.PI/180.0));
            y = (float)(r*Math.sin(210.0*Math.PI/180.0));
            j = text_detail_p2;
        }else if(point == 3){
            // Tower B at 330
            x = (float)(r*Math.cos(330.0*Math.PI/180.0));
            y = (float)(r*Math.sin(330.0*Math.PI/180.0));
            j = text_detail_p3;
        }else if(point == 4){
            // Tower C at 90
            x = (float)(r*Math.cos(90.0*Math.PI/180.0));
            y = (float)(r*Math.sin(90.0*Math.PI/180.0));
            j = text_detail_p4;
        }
        // verify x and y are not crazy
        double eps = Math.sqrt((x-xa)*(x-xa)+(y-ya)*(y-ya));
        if(eps > 0.01){
            // why would you do that
            // should i throw an error
            return;
        }
        // save x, y, z
        j.setText(xa+","+ya+","+za);
    }//GEN-LAST:event_button_detailSavePointActionPerformed

    private void button_readInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_readInActionPerformed
        // TODO add your handling code here:
        File out = null;
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
        "Tab Delimit Files", "txt");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           out = chooser.getSelectedFile();
        }
        try{
            BufferedReader br = new BufferedReader(new FileReader(out));
            ArrayList<String> commands = this.getEepromState().readIn(br);
            SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
            new Thread(sci).start();                                                          
        } catch (Exception ex) {
            Logger.getLogger(EasyDeltaPie.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }//GEN-LAST:event_button_readInActionPerformed

    private void button_detail_ApActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_ApActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        float towers[] = getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();
        if(towers == null){
            return;
        }
        towers[0] = towers[0] - tweak;
        ArrayList<String> commands = new ArrayList<>();
        commands.addAll(getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjustEepromCommands(towers));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_ApActionPerformed

    private void button_detail_AmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_AmActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        float towers[] = getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();
        if(towers == null){
            return;
        }
        towers[0] = towers[0] + tweak;
        ArrayList<String> commands = new ArrayList<>();
        commands.addAll(getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjustEepromCommands(towers));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_AmActionPerformed

    private void button_detail_BpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_BpActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        float towers[] = getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();        
        if(towers == null){
            return;
        }
        towers[1] = towers[1] - tweak;
        ArrayList<String> commands = new ArrayList<>();
        commands.addAll(getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjustEepromCommands(towers));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_BpActionPerformed

    private void button_detail_BmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_BmActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        float towers[] = getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();        
        if(towers == null){
            return;
        }
        towers[1] = towers[1] + tweak;
        ArrayList<String> commands = new ArrayList<>();
        commands.addAll(getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjustEepromCommands(towers));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_BmActionPerformed

    private void button_detail_CpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_CpActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        float towers[] = getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();        
        if(towers == null){
            return;
        }
        towers[2] = towers[2] - tweak;
        ArrayList<String> commands = new ArrayList<>();
        commands.addAll(getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjustEepromCommands(towers));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_CpActionPerformed

    private void button_detail_CmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_CmActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        float towers[] = getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjusts();        
        if(towers == null){
            return;
        }
        towers[2] = towers[2] + tweak;
        ArrayList<String> commands = new ArrayList<>();
        commands.addAll(getSelectedComPort().getFirmwareParser().getEepromState().getTowerStopAdjustEepromCommands(towers));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_CmActionPerformed

    private void button_detail_RpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_RpActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        EepromValue ev = this.getSelectedComPort().getFirmwareParser().getEepromState().getHorizontalRodRadius();
        float horRad = ev.getValueAsFloat();
        if(Float.isNaN(horRad)){
            return;
        }
        ArrayList<String> commands = new ArrayList<>();
        commands.add(getSelectedComPort().getFirmwareParser().getEepromState().getEepromWriteCommand(ev, ""+(horRad + tweak)));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();

    }//GEN-LAST:event_button_detail_RpActionPerformed

    private void button_detail_RmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_detail_RmActionPerformed
        float tweak = Float.NaN;
        try{
            tweak = Float.parseFloat(this.text_detail_TowerTweak.getText());
        }catch(NumberFormatException e){}
        if(Float.isNaN(tweak)){
            // throw an error? should be error trapped at set
            return ;
        }
        EepromValue ev = this.getSelectedComPort().getFirmwareParser().getEepromState().getHorizontalRodRadius();
        float horRad = ev.getValueAsFloat();
        if(Float.isNaN(horRad)){
            return;
        }
        ArrayList<String> commands = new ArrayList<>();
        commands.add(getSelectedComPort().getFirmwareParser().getEepromState().getEepromWriteCommand(ev, ""+(horRad - tweak)));
        commands.add("G28");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();
    }//GEN-LAST:event_button_detail_RmActionPerformed
    private BedProbePoint getCoordFromLabel(JLabel label){
        StringTokenizer stok = new StringTokenizer(label.getText(),",");
        float za = Float.NaN;
        float xa = Float.NaN;
        float ya = Float.NaN;
        try{
            xa = Float.parseFloat(stok.nextToken().trim());
            ya = Float.parseFloat(stok.nextToken().trim());
            za = Float.parseFloat(stok.nextToken().trim());
        }catch(NumberFormatException e){}
        if(Float.isNaN(xa) || Float.isNaN(ya) || Float.isNaN(za)){
            // throw an error? should be error trapped at set
            return null;
        }
        BedProbePoint out = new BedProbePoint(xa,ya,null);
        out.addRawProbeZ(za);
        out.runStatisticalAnalysis(1.0);
        return out;
    }
    private void moveToDetailPoint(){
        int perRadius = this.slider_detailRadPer.getValue();
        double r = getSelectedComPort().getFirmwareParser().getEepromState().getMaxBuildRadius().getValueAsFloat()*perRadius/100.0;
        int point = this.slider_detailPoint.getValue();
        float x = 0.0f;
        float y = 0.0f;
        float z = Float.NaN;
        try{
            z = Float.parseFloat(this.text_detailHeight.getText().trim());
        }catch(Exception e){}
        if(Float.isNaN(z)){
            // throw an error? should be error trapped at set
            return;
        }
        // point 1 just keep at zero
        if(point == 2){
            // Tower A at 210
            x = (float)(r*Math.cos(210.0*Math.PI/180.0));
            y = (float)(r*Math.sin(210.0*Math.PI/180.0));
        }else if(point == 3){
            // Tower B at 330
            x = (float)(r*Math.cos(330.0*Math.PI/180.0));
            y = (float)(r*Math.sin(330.0*Math.PI/180.0));
        }else if(point == 4){
            // Tower C at 90
            x = (float)(r*Math.cos(90.0*Math.PI/180.0));
            y = (float)(r*Math.sin(90.0*Math.PI/180.0));            
        }
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("G90");
        if(!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(z)){
            String move = "G0 X"+x+" Y"+y+" Z"+z;
            commands.add(move);
        }else{
            //<>< should we let them know they didnt make sense?
        }
        commands.add("M114");
        SingleCommandControl sci = new SingleCommandControl(this.getSelectedComPort(), commands);
        new Thread(sci).start();                                              
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(EasyDeltaPie.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new EasyDeltaPie().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_Center;
    private javax.swing.JToggleButton button_Connect;
    private javax.swing.JButton button_GoToCart;
    private javax.swing.JButton button_GoToRad;
    private javax.swing.JButton button_Rneg;
    private javax.swing.JButton button_Rpos;
    private javax.swing.JButton button_Tneg;
    private javax.swing.JButton button_Tpos;
    private javax.swing.JButton button_Xneg;
    private javax.swing.JButton button_Xpos;
    private javax.swing.JButton button_Yneg;
    private javax.swing.JButton button_Ypos;
    private javax.swing.JButton button_Zneg;
    private javax.swing.JButton button_Zneg1;
    private javax.swing.JButton button_Zneg2;
    private javax.swing.JButton button_Zpos;
    private javax.swing.JButton button_Zpos1;
    private javax.swing.JButton button_Zpos2;
    private javax.swing.JButton button_autoLevel;
    private javax.swing.JButton button_cal_Rods;
    private javax.swing.JButton button_cal_step;
    private javax.swing.JButton button_cal_updateRods;
    private javax.swing.JButton button_cal_updateSteps;
    private javax.swing.JButton button_cal_updateTowerAngle;
    private javax.swing.JButton button_calcTowerOffsets;
    private javax.swing.JButton button_command;
    private javax.swing.JButton button_detailGoto;
    private javax.swing.JButton button_detailSavePoint;
    private javax.swing.JButton button_detail_Am;
    private javax.swing.JButton button_detail_Ap;
    private javax.swing.JButton button_detail_Bm;
    private javax.swing.JButton button_detail_Bp;
    private javax.swing.JButton button_detail_Cm;
    private javax.swing.JButton button_detail_Cp;
    private javax.swing.JButton button_detail_Rm;
    private javax.swing.JButton button_detail_Rp;
    private javax.swing.JButton button_detail_clearAll;
    private javax.swing.JButton button_detail_setAllZero;
    private javax.swing.JButton button_eeprom;
    private javax.swing.JButton button_homeAll;
    private javax.swing.JButton button_homeAll1;
    private javax.swing.JButton button_homeAll2;
    private javax.swing.JToggleButton button_limitCheck;
    private javax.swing.JButton button_readIn;
    private javax.swing.JButton button_rescanComs;
    private javax.swing.JButton button_setProbeUsingCurZ;
    private javax.swing.JButton button_startProbeCheck;
    private javax.swing.JButton button_updateHeight;
    private javax.swing.JButton button_updatePrintRadius;
    private javax.swing.JButton button_updateProbeHeight;
    private javax.swing.JButton button_updateProbeStartHeight;
    private javax.swing.JButton button_updateRadiusAtZero;
    private javax.swing.JButton button_updateRodLength;
    private javax.swing.JButton button_writeOut;
    private javax.swing.JCheckBox check_filter;
    private javax.swing.JCheckBox check_scroll;
    private javax.swing.JComboBox combo_baud;
    private javax.swing.JComboBox combo_coms;
    private javax.swing.JComboBox combo_eeprom;
    private javax.swing.JPanel connectPanel;
    private javax.swing.JPanel detailPanel;
    private javax.swing.JPanel gantryPanel;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JLabel label_limit_A1;
    private javax.swing.JLabel label_limit_A_state;
    private javax.swing.JLabel label_limit_B1;
    private javax.swing.JLabel label_limit_B_state;
    private javax.swing.JLabel label_limit_C1;
    private javax.swing.JLabel label_limit_C_state;
    private javax.swing.JLabel label_limit_D1;
    private javax.swing.JLabel label_limit_D_state;
    private javax.swing.JPanel levelPanel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel motionPanel;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JProgressBar progress_level;
    private javax.swing.JScrollPane scroll_deltaLevel;
    private javax.swing.JScrollPane scroll_output;
    private javax.swing.JPanel setupPanel;
    private javax.swing.JSlider slider_detailPoint;
    private javax.swing.JSlider slider_detailRadPer;
    private javax.swing.JSlider slider_radPer;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JSpinner text_Nudge_Distance;
    private javax.swing.JSpinner text_Nudge_Distance1;
    private javax.swing.JSpinner text_Nudge_Distance2;
    private javax.swing.JTextField text_cal_angleA;
    private javax.swing.JTextField text_cal_angleB;
    private javax.swing.JLabel text_cal_angleC;
    private javax.swing.JLabel text_cal_calcA;
    private javax.swing.JLabel text_cal_calcB;
    private javax.swing.JLabel text_cal_calcC;
    private javax.swing.JLabel text_cal_curStepsMM;
    private javax.swing.JLabel text_cal_horzRad;
    private javax.swing.JTextField text_cal_radius;
    private javax.swing.JTextField text_cal_radiusA;
    private javax.swing.JTextField text_cal_radiusB;
    private javax.swing.JTextField text_cal_radiusC;
    private javax.swing.JLabel text_cal_rodA;
    private javax.swing.JLabel text_cal_rodB;
    private javax.swing.JLabel text_cal_rodC;
    private javax.swing.JLabel text_cal_stepsAdjusted;
    private javax.swing.JTextField text_cal_zActual;
    private javax.swing.JTextField text_cal_zExpected;
    private javax.swing.JTextField text_command;
    private javax.swing.JTextField text_detailHeight;
    private javax.swing.JTextField text_detail_TowerTweak;
    private javax.swing.JLabel text_detail_p1;
    private javax.swing.JLabel text_detail_p2;
    private javax.swing.JLabel text_detail_p3;
    private javax.swing.JLabel text_detail_p4;
    private javax.swing.JTextField text_eeprom;
    private javax.swing.JTextPane text_gantryDesc1;
    private javax.swing.JTextField text_level_cov;
    private javax.swing.JTextArea text_level_output;
    private javax.swing.JTextField text_level_taps;
    private javax.swing.JTextArea text_output;
    private javax.swing.JTextField text_posR;
    private javax.swing.JTextField text_posT;
    private javax.swing.JTextField text_posX;
    private javax.swing.JLabel text_posX1;
    private javax.swing.JLabel text_posX2;
    private javax.swing.JTextField text_posY;
    private javax.swing.JLabel text_posY1;
    private javax.swing.JLabel text_posY2;
    private javax.swing.JTextField text_posZ;
    private javax.swing.JLabel text_posZ1;
    private javax.swing.JLabel text_posZ2;
    private javax.swing.JTextField text_setup_height;
    private javax.swing.JTextField text_setup_maxPrintRadius;
    private javax.swing.JTextField text_setup_probeHeight;
    private javax.swing.JTextField text_setup_probeStartHeight;
    private javax.swing.JTextField text_setup_rodAtZero;
    private javax.swing.JTextField text_setup_rodLength;
    private javax.swing.JToggleButton toggle_AllowNegHeightDetail;
    private javax.swing.JToggleButton toggle_allowNegativeOnProbe;
    private javax.swing.JPanel tolerancePanel;
    // End of variables declaration//GEN-END:variables

    public JComboBox getCombo_coms() {
        return combo_coms;
    }
    private EepromState getEepromState(){
        return getSelectedComPort().getFirmwareParser().getEepromState();
    }
    private DeltaComPort getSelectedComPort() {
        DeltaComPort dcp = null;
        try{
            dcp = (DeltaComPort)this.combo_coms.getSelectedItem();
        }catch(Exception e){
            JOptionPane.showConfirmDialog(this, "No Port Selected");
        }
        return dcp;
    }

    public int getBaudRate() {
        int out = 250000;
        try{
            out = Integer.parseInt(this.combo_baud.getSelectedItem().toString());
        }catch(NumberFormatException e){}
        return out;
    }

    public int getFirmareType() {
        return Firmware.REPETIER;
    }

    public boolean getFilterActive() {
        return this.check_filter.isSelected();
    }

    public void setActionButtonsActive(boolean view) {
        view = view && warmedUp;
        for(Component cmp : commandButtons){
            cmp.setEnabled(view);
        }
        if(!view && !immuneToEnable.isEmpty()){
            for(Component c: immuneToEnable){
                c.setEnabled(true);
            }
        }
    }
    // done at start of connect
    public void setEepromValues(EepromState eeprom) {
        this.combo_eeprom.setModel(eeprom);
        combo_eepromActionPerformed(null);
        // update other textboxes that would like this data
        if(this.text_setup_height.getText().trim().isEmpty() && eeprom.getHomedHeight() != null){
            this.text_setup_height.setText(eeprom.getHomedHeight().getValue());
        }
    }
    public void fireEepromValueChanged(EepromState eeprom) {
        if(!warmedUp){return;}
        float[] rods = this.getEepromState().getDiagonalRodLenghts();
        float rodAvg = (rods[0]+rods[1]+rods[2])/3;
        float[] angles = this.getEepromState().getTowerAngleAdjusts();
        // updated text boxes that requre eeprom data
        try{this.text_setup_height.setText(eeprom.getHomedHeight().getValue());}catch(Exception e){};
        try{this.text_setup_maxPrintRadius.setText(eeprom.getMaxBuildRadius().getValue());}catch(Exception e){};
        try{this.text_setup_probeStartHeight.setText(eeprom.getProbeZStartHeight().getValue());}catch(Exception e){};
        try{this.text_setup_probeHeight.setText(eeprom.getProbeZHeight().getValue());}catch(Exception e){};
        try{this.text_setup_rodLength.setText(""+rodAvg);}catch(Exception e){};
        try{this.text_setup_rodAtZero.setText(eeprom.getHorizontalRodRadius().getValue());}catch(Exception e){};
        try{this.text_cal_curStepsMM.setText(eeprom.getStepsPerMM().getValue());}catch(Exception e){};
        try{this.text_cal_horzRad.setText(eeprom.getHorizontalRodRadius().getValue());}catch(Exception e){};
        try{this.text_cal_rodA.setText(""+rods[0]);}catch(Exception e){};
        try{this.text_cal_rodB.setText(""+rods[1]);}catch(Exception e){};
        try{this.text_cal_rodC.setText(""+rods[2]);}catch(Exception e){};
        try{this.text_cal_angleA.setText(""+angles[0]);}catch(Exception e){};
        try{this.text_cal_angleB.setText(""+angles[1]);}catch(Exception e){};
        try{this.text_cal_angleC.setText(""+angles[2]);}catch(Exception e){};
    }

    public void addToOutput(String s) {
        this.text_output.append(s);
        if(this.check_scroll.isSelected()){
            this.scroll_output.getVerticalScrollBar().setValue(this.scroll_output.getVerticalScrollBar().getMaximum());
        }    
    }

    private void resetConnection() {
        JOptionPane.showConfirmDialog(this, "Fatal error reseting connection state");
        DeltaComPort dcp = getSelectedComPort();
        if(dcp == null){
            return;
        }
        // want to disconnect
        dcp.close();
        updatedConnectedViews(false);
        button_Connect.setSelected(true);
        updatedConnectedViews(true);
        // start connection
        Thread portThread = new Thread(dcp);
        portThread.start();
        
    }

    public void setEndStopStates(EndStopState state) {
        int i = 0;
        for(EndStopValue ess : state.getValues()){
            switch (i) {
                case 0:
                    this.label_limit_A1.setText(ess.toString());
                    colorLimitLabel(this.label_limit_A_state,ess.isHigh());
                    break;
                case 1:
                    this.label_limit_B1.setText(ess.toString());
                    colorLimitLabel(this.label_limit_B_state,ess.isHigh());
                    break;
                case 2:
                    this.label_limit_C1.setText(ess.toString());
                    colorLimitLabel(this.label_limit_C_state,ess.isHigh());
                    break;
                case 3:
                    this.label_limit_D1.setText(ess.toString());
                    colorLimitLabel(this.label_limit_D_state,ess.isHigh());
                    break;
                default:
                    break;
            }
            i++;
        }
        
    }
    private void colorLimitLabel(JLabel aLabel, boolean high){
        if(high){
            aLabel.setText("CLOSE");
            aLabel.setBackground(Color.red);
        }else{
            aLabel.setText("OPEN");
            aLabel.setBackground(Color.green);
            
        }
    }

    public void updateAutoLevelStatus(int i, String s) {
        this.progress_level.setValue(i);
        if(s != null){
            this.text_level_output.append(s+"\n");
            if(debug){
                System.out.println(s);
            }
        }
        if(s != null && i == 100){
            summary_level.append(s.replace("\t", " ")).append("\n");
        }
    }
    public String getStatusString(){
        return summary_level.toString();
    }

    public void setLocationData(PositionState state) {
        float x = state.getX();
        float y = state.getY();
        float z = state.getZ();
        float r = (float)Math.round(Math.sqrt(x*x+y*y)* 100) / 100;
        float t = (float)Math.round((180.0/Math.PI*Math.atan2(y, x))* 100) / 100;        
        this.text_posX.setText(""+x);
        this.text_posY.setText(""+y);
        this.text_posZ.setText(""+z);
        this.text_posR.setText(""+r);
        this.text_posT.setText(""+t);
        this.text_posX1.setText(""+x);
        this.text_posY1.setText(""+y);
        this.text_posZ1.setText(""+z);
        this.text_posX2.setText(""+x);
        this.text_posY2.setText(""+y);
        this.text_posZ2.setText(""+z);
    }

    private float[] crossP(float[] a, float[] b) {
        float[] out = new float[3];
        out[0] = a[1]*b[2]-a[2]*b[1];
        out[1] = a[2]*b[0]-a[0]*b[2];
        out[2] = a[0]*b[1]-a[1]*b[0];
        return out;
    }

    private String f(double horizontalRodRadius) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
