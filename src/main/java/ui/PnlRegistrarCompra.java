
package ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import conexion.conexionBD;

public class PnlRegistrarCompra extends javax.swing.JPanel {

    /**
     * Creates new form PanelRegistrarCompra
     */
    public PnlRegistrarCompra() {
        initComponents();
        
        txtTotalDescuentos.setText("0.00");
txtTotalDescuentos.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
    @Override
    public void insertUpdate(javax.swing.event.DocumentEvent e) { calcularTotalFinal(); }
    @Override
    public void removeUpdate(javax.swing.event.DocumentEvent e) { calcularTotalFinal(); }
    @Override
    public void changedUpdate(javax.swing.event.DocumentEvent e) { calcularTotalFinal(); }
});
        
        configurarTablaInsumos();
        
     String numeroCompra = txtNumeroCompra.getText();
     String fecha = txtFecha.getText();
     String proveedorSeleccionado = cmbProveedor.getSelectedItem().toString();
     String formaPagoSeleccionada = cmbFormaPago.getSelectedItem().toString();
        
    String numeroAuto = "COM-00085"; 
    txtNumeroCompra.setText(numeroAuto);
    txtNumeroCompra.setEditable(false);
    txtNumeroCompra.setEnabled(false); // Bloqueado para evitar modificaciones manuales

    // --- 2. CABECERA: FECHA ACTUAL ---
    java.time.LocalDate fechaActual = java.time.LocalDate.now();
    java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    txtFecha.setText(fechaActual.format(formato));
    txtFecha.setEditable(false);
    txtFecha.setEnabled(false);

    // --- 3. CABECERA: PROVEEDOR ---
    cmbProveedor.removeAllItems();
    cmbProveedor.addItem("Buscar Proveedor...");
    

    // --- 4. CABECERA: FORMA DE PAGO ---
    cmbFormaPago.removeAllItems();
    cmbFormaPago.addItem("CONTADO");
    cmbFormaPago.addItem("CRÉDITO 30 DÍAS");
    cmbFormaPago.addItem("CRÉDITO 60 DÍAS");
    cmbFormaPago.addItem("TRANSFERENCIA");
    cmbFormaPago.addItem("CHEQUE");
    }
    
    private void configurarTablaInsumos() {
    
    javax.swing.table.DefaultTableModel modeloTabla = new javax.swing.table.DefaultTableModel(
        new Object [][] {},
        new String [] {
            "Código / Insumo", "Descripción", "Cantidad Comprada", "Precio de Compra", "Subtotal", "IVA de la línea", "Total de la línea"
        }
    ) {
        
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; 
        }
    };
    
    
    jTableInsumos.setModel(modeloTabla);
}
    public void calcularTotalesGenerales() {
    javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) jTableInsumos.getModel();
    double subtotalGeneral = 0.0;
    
    
    for (int i = 0; i < modelo.getRowCount(); i++) {
        try {
           
            Object valorCelda = modelo.getValueAt(i, 4);
            if (valorCelda != null) {
                double subtotalFila = Double.parseDouble(valorCelda.toString().replace(",", "."));
                subtotalGeneral += subtotalFila;
            }
        } catch (NumberFormatException e) {
            
        }
    }
    
    
    txtSubtotalGeneral.setText(String.format("%.2f", subtotalGeneral));
    
    
}
    public void calcularTotalFinal() {
    try {
        
        double subtotalGeneral = 0.0;
        if (!txtSubtotalGeneral.getText().trim().isEmpty()) {
            subtotalGeneral = Double.parseDouble(txtSubtotalGeneral.getText().trim().replace(",", "."));
        }
        
        double descuentos = 0.0;
        String textDescuentos = txtTotalDescuentos.getText().trim();
        if (!textDescuentos.isEmpty()) {
            descuentos = Double.parseDouble(textDescuentos.replace(",", "."));
        }
        
        
        double baseImponible = subtotalGeneral - descuentos;
        if (baseImponible < 0) baseImponible = 0;
        
        double totalIva = baseImponible * 0.12;
        
       
        txtTotalIva.setText(String.format("%.2f", totalIva));
        // 3. Calcular Total Final
        double totalFinal = baseImponible + totalIva;
        
        
          txtTotalFinal.setText(String.format("%.2f",totalFinal));
    } catch (NumberFormatException e) {
        
    }
    
    
}
    private void limpiarPanelCompra() {
    // Vaciar la tabla de insumos
    javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) jTableInsumos.getModel();
    modelo.setRowCount(0);
    
    // Reiniciar campos de totales
    txtSubtotalGeneral.setText("0.00");
    txtTotalDescuentos.setText("0.00");
    txtTotalIva.setText("0.00");
    txtTotalFinal.setText("0.00");
    
    // Opcional: Reiniciar proveedor o forma de pago si lo deseas
    if (cmbProveedor.getItemCount() > 0) {
        cmbProveedor.setSelectedIndex(0);
    }
}
    public class CompraDAO {

    
    public boolean registrarCompra(int idProveedor, String fecha, String formaPago, 
                                   double subtotalGen, double totalDesc, double totalIva, double totalFin, 
                                   DefaultTableModel modeloTabla) {
        
        Connection conexion = null;
        PreparedStatement psCabecera = null;
        PreparedStatement psDetalle = null;
        ResultSet rsGenerado = null;
        
        String sqlCabecera = "INSERT INTO compras (id_proveedor, fecha, forma_pago, subtotal, descuento, iva, total) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_compras (id_compra, codigo_insumo, cantidad, precio_unitario, subtotal, iva, total_linea) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try {
            
            conexion = conexionBD.obtenerConexion(); 
            
           
            conexion.setAutoCommit(false);
            
            // 2. Insertar la Cabecera de la Compra
            psCabecera = conexion.prepareStatement(sqlCabecera, PreparedStatement.RETURN_GENERATED_KEYS);
            psCabecera.setInt(1, idProveedor);
            psCabecera.setString(2, fecha);
            psCabecera.setString(3, formaPago);
            psCabecera.setDouble(4, subtotalGen);
            psCabecera.setDouble(5, totalDesc);
            psCabecera.setDouble(6, totalIva);
            psCabecera.setDouble(7, totalFin);
            
            psCabecera.executeUpdate();
            
            // Obtener el ID de la compra que se acaba de generar
            rsGenerado = psCabecera.getGeneratedKeys();
            int idCompraGenerado = 0;
            if (rsGenerado.next()) {
                idCompraGenerado = rsGenerado.getInt(1);
            } else {
                throw new SQLException("No se pudo obtener el ID de la compra generada.");
            }
            
            // 3. Insertar los Detalles (cada fila de la tabla jTableInsumos)
            psDetalle = conexion.prepareStatement(sqlDetalle);
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                String codigoInsumo = modeloTabla.getValueAt(i, 0).toString(); // Columna 0: Código
                double cantidad = Double.parseDouble(modeloTabla.getValueAt(i, 2).toString()); // Columna 2: Cantidad
                double precio = Double.parseDouble(modeloTabla.getValueAt(i, 3).toString()); // Columna 3: Precio
                double subtotalFila = Double.parseDouble(modeloTabla.getValueAt(i, 4).toString()); // Columna 4: Subtotal
                double ivaFila = Double.parseDouble(modeloTabla.getValueAt(i, 5).toString()); // Columna 5: IVA
                double totalFila = Double.parseDouble(modeloTabla.getValueAt(i, 6).toString()); // Columna 6: Total línea
                
                psDetalle.setInt(1, idCompraGenerado);
                psDetalle.setString(2, codigoInsumo);
                psDetalle.setDouble(3, cantidad);
                psDetalle.setDouble(4, precio);
                psDetalle.setDouble(5, subtotalFila);
                psDetalle.setDouble(6, ivaFila);
                psDetalle.setDouble(7, totalFila);
                
                psDetalle.addBatch(); // Agregamos al lote
            }
            
            psDetalle.executeBatch(); 
            
            
            conexion.commit();
            return true;
            
        } catch (SQLException e) {
           
            if (conexion != null) {
                try {
                    conexion.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            
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

        jLabel1 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        txtNumeroCompra = new javax.swing.JTextField();
        txtFecha = new javax.swing.JTextField();
        cmbProveedor = new javax.swing.JComboBox<>();
        cmbFormaPago = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableInsumos = new javax.swing.JTable();
        btnAgregarInsumo = new javax.swing.JButton();
        btnEliminarSeleccion = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        txtSubtotalGeneral = new javax.swing.JTextField();
        txtTotalDescuentos = new javax.swing.JTextField();
        txtTotalIva = new javax.swing.JTextField();
        txtTotalFinal = new javax.swing.JTextField();
        btnConfirmarCompra = new javax.swing.JButton();

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Registrar Compra ");

        jPanel8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel9.setText("Cabecera de la Compra");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(269, 269, 269))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        txtNumeroCompra.setEditable(false);
        txtNumeroCompra.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtNumeroCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNumeroCompraActionPerformed(evt);
            }
        });

        txtFecha.setEditable(false);
        txtFecha.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtFecha.setText("Fecha: ");
        txtFecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFechaActionPerformed(evt);
            }
        });

        cmbProveedor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbProveedorActionPerformed(evt);
            }
        });

        cmbFormaPago.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Contado", "Credito 30 dias", "Credito 60 dias", "Transferencia", "Cheque" }));
        cmbFormaPago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFormaPagoActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("Número de Compra:");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setText("Fecha:");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setText("Proveedor: ");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setText("Forma de pago:");

        jLabel14.setText("(AUTOMATICO)");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtNumeroCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(58, 58, 58)
                                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(cmbProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(41, 41, 41)
                                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(cmbFormaPago, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(33, 33, 33)
                                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(jLabel14)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12)
                    .addComponent(jLabel13))
                .addGap(4, 4, 4)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNumeroCompra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbProveedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbFormaPago, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addGap(0, 9, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 849, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 245, Short.MAX_VALUE)
        );

        jTableInsumos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Codigo de Insumo", "Descripción", "Cantidad Comprada", "Precio de compra", "Subtotal", "IVA de la linea", "Total de la linea"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableInsumos);

        btnAgregarInsumo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnAgregarInsumo.setText("+Agregar Insumo");
        btnAgregarInsumo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarInsumoActionPerformed(evt);
            }
        });

        btnEliminarSeleccion.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnEliminarSeleccion.setText("Eliminar Selección");
        btnEliminarSeleccion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarSeleccionActionPerformed(evt);
            }
        });

        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jPanel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel15.setText("TOTALES");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel16.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel16.setText("Subtotal general");

        jLabel17.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel17.setText("Total Descuentos");

        jLabel18.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel18.setText("Total IVA");

        jLabel19.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel19.setText("Total Final");

        txtSubtotalGeneral.setEditable(false);
        txtSubtotalGeneral.setBackground(new java.awt.Color(204, 204, 204));
        txtSubtotalGeneral.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtTotalDescuentos.setBackground(new java.awt.Color(204, 204, 204));
        txtTotalDescuentos.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        txtTotalDescuentos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTotalDescuentosActionPerformed(evt);
            }
        });

        txtTotalIva.setEditable(false);
        txtTotalIva.setBackground(new java.awt.Color(204, 204, 204));
        txtTotalIva.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtTotalFinal.setEditable(false);
        txtTotalFinal.setBackground(new java.awt.Color(204, 204, 204));
        txtTotalFinal.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        btnConfirmarCompra.setBackground(new java.awt.Color(0, 204, 0));
        btnConfirmarCompra.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnConfirmarCompra.setText("Confirmar Compra");
        btnConfirmarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarCompraActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtSubtotalGeneral, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTotalDescuentos, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(txtTotalIva, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(42, 42, 42)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTotalFinal, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnConfirmarCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(jLabel18)
                            .addComponent(jLabel17)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSubtotalGeneral, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTotalDescuentos, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTotalIva, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTotalFinal, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(btnConfirmarCompra, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 51, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(btnAgregarInsumo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnEliminarSeleccion))
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 837, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAgregarInsumo, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminarSeleccion, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(140, 140, 140)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(223, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtNumeroCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNumeroCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNumeroCompraActionPerformed

    private void txtFechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFechaActionPerformed
java.time.LocalDate fechaActual = java.time.LocalDate.now();
java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
txtFecha.setText(fechaActual.format(formato));
txtFecha.setEditable(false);
txtFecha.setEnabled(false);        // TODO add your handling code here:
    }//GEN-LAST:event_txtFechaActionPerformed

    private void cmbProveedorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbProveedorActionPerformed
        // Cargar opciones en Forma de Pago
       cmbProveedor.removeAllItems();
       cmbProveedor.addItem("Seleccione un Proveedor...");       // TODO add your handling code here:
    }//GEN-LAST:event_cmbProveedorActionPerformed

    private void cmbFormaPagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFormaPagoActionPerformed
cmbFormaPago.removeAllItems();
cmbFormaPago.addItem("CONTADO");
cmbFormaPago.addItem("CRÉDITO 30 DÍAS");
cmbFormaPago.addItem("CRÉDITO 60 DÍAS");
cmbFormaPago.addItem("TRANSFERENCIA");
cmbFormaPago.addItem("CHEQUE");        // TODO add your handling code here:
    }//GEN-LAST:event_cmbFormaPagoActionPerformed

    private void btnEliminarSeleccionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarSeleccionActionPerformed

        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) jTableInsumos.getModel();
    
    // 2. Obtener la fila que el usuario seleccionó en la tabla
    int filaSeleccionada = jTableInsumos.getSelectedRow();
    
    // 3. Validar que realmente haya una fila seleccionada
    if (filaSeleccionada >= 0) {
        // Eliminar la fila del modelo de la tabla
        modelo.removeRow(filaSeleccionada);
        
        
        
        javax.swing.JOptionPane.showMessageDialog(this, "Insumo eliminado de la lista.", "Información", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    } else {
        // Si no seleccionó ninguna fila, mostrar advertencia
        javax.swing.JOptionPane.showMessageDialog(this, "Por favor, seleccione de la tabla el insumo que desea eliminar.", "Advertencia", javax.swing.JOptionPane.WARNING_MESSAGE);
    }
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEliminarSeleccionActionPerformed

    private void btnAgregarInsumoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarInsumoActionPerformed
        DlgAgregarInsumo dialog = new DlgAgregarInsumo((javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(this), true);
        dialog.setVisible(true);
    }//GEN-LAST:event_btnAgregarInsumoActionPerformed

    private void btnConfirmarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarCompraActionPerformed

        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) jTableInsumos.getModel();
    
    // 1. Validar que la tabla tenga al menos un insumo agregado
    if (modelo.getRowCount() == 0) {
        javax.swing.JOptionPane.showMessageDialog(this, "Debe agregar al menos un insumo a la compra.", "Advertencia", javax.swing.JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // 2. Validar proveedor
    if (cmbProveedor.getSelectedIndex() <= 0) {
        javax.swing.JOptionPane.showMessageDialog(this, "Por favor, seleccione un proveedor válido.", "Advertencia", javax.swing.JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    try {
        // 3. Obtener los datos de la interfaz para enviarlos al DAO
        int idProveedor = cmbProveedor.getSelectedIndex();
        String fecha = txtFecha.getText().trim();
        String formaPago = cmbFormaPago.getSelectedItem().toString(); // Ajusta según tu combo de forma de pago
        
        double subtotalGen = Double.parseDouble(txtSubtotalGeneral.getText().trim());
        double totalDesc = Double.parseDouble(txtTotalDescuentos.getText().trim());
        double totalIva = Double.parseDouble(txtTotalIva.getText().trim());
        double totalFin = Double.parseDouble(txtTotalFinal.getText().trim());
        
        // 4. Instanciar el DAO y registrar
        CompraDAO dao = new CompraDAO();
        boolean exito = dao.registrarCompra(idProveedor, fecha, formaPago, subtotalGen, totalDesc, totalIva, totalFin, modelo);
        
        if (exito) {
            javax.swing.JOptionPane.showMessageDialog(this, "¡Compra registrada y guardada en la base de datos con éxito!", "Éxito", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            limpiarPanelCompra();
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Ocurrió un error al intentar guardar la compra.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error en los datos: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnConfirmarCompraActionPerformed

    private void txtTotalDescuentosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTotalDescuentosActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTotalDescuentosActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregarInsumo;
    private javax.swing.JButton btnConfirmarCompra;
    private javax.swing.JButton btnEliminarSeleccion;
    private javax.swing.JComboBox<String> cmbFormaPago;
    private javax.swing.JComboBox<String> cmbProveedor;
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
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableInsumos;
    private javax.swing.JTextField txtFecha;
    private javax.swing.JTextField txtNumeroCompra;
    private javax.swing.JTextField txtSubtotalGeneral;
    private javax.swing.JTextField txtTotalDescuentos;
    private javax.swing.JTextField txtTotalFinal;
    private javax.swing.JTextField txtTotalIva;
    // End of variables declaration//GEN-END:variables
}
