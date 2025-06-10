import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.List;

abstract class Expense {
    private int id;
    private double amount;
    private java.sql.Date date;
    private String description;

    public Expense(int id, double amount, java.sql.Date date, String description) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }
    public Expense(double amount, java.sql.Date date, String description) {
        this(-1, amount, date, description);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public java.sql.Date getDate() { return date; }
    public void setDate(java.sql.Date date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public abstract String getType();

    public abstract String getDetails();

    public abstract void setDetailsFromString(String details);
}

class FoodExpense extends Expense {
    private String restaurant;

    public FoodExpense(int id, double amount, java.sql.Date date, String description, String restaurant) {
        super(id, amount, date, description);
        this.restaurant = restaurant;
    }

    public FoodExpense(double amount, java.sql.Date date, String description, String restaurant) {
        super(amount, date, description);
        this.restaurant = restaurant;
    }

    @Override
    public String getType() { return "Food"; }

    public String getRestaurant() { return restaurant; }
    public void setRestaurant(String restaurant) { this.restaurant = restaurant; }

    @Override
    public String getDetails() { return restaurant; }

    @Override
    public void setDetailsFromString(String details) {
        setRestaurant(details);
    }
}

class TravelExpense extends Expense {
    private String destination;
    private String transportMode;

    public TravelExpense(int id, double amount, java.sql.Date date, String description, String destination, String transportMode) {
        super(id, amount, date, description);
        this.destination = destination;
        this.transportMode = transportMode;
    }

    public TravelExpense(double amount, java.sql.Date date, String description, String destination, String transportMode) {
        super(amount, date, description);
        this.destination = destination;
        this.transportMode = transportMode;
    }

    @Override
    public String getType() { return "Travel"; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }

    @Override
    public String getDetails() {
        return destination + ", " + transportMode;
    }

    @Override
    public void setDetailsFromString(String details) {
        String[] parts = details.split(",\\s*");
        if(parts.length == 2) {
            setDestination(parts[0]);
            setTransportMode(parts[1]);
        } else {
            setDestination("");
            setTransportMode("");
        }
    }
}

class UtilityExpense extends Expense {
    private String utilityType;

    public UtilityExpense(int id, double amount, java.sql.Date date, String description, String utilityType) {
        super(id, amount, date, description);
        this.utilityType = utilityType;
    }

    public UtilityExpense(double amount, java.sql.Date date, String description, String utilityType) {
        super(amount, date, description);
        this.utilityType = utilityType;
    }

    @Override
    public String getType() { return "Utility"; }

    public String getUtilityType() { return utilityType; }
    public void setUtilityType(String utilityType) { this.utilityType = utilityType; }

    @Override
    public String getDetails() { return utilityType; }

    @Override
    public void setDetailsFromString(String details) {
        setUtilityType(details);
    }
}

class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found.", e);
            }
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        }
        return connection;
    }

    public static void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }
}

class ExpenseDAO {
    public void insertExpense(Expense exp) throws SQLException {
        String sql = "INSERT INTO expenses (amount, date, description, type, details) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDouble(1, exp.getAmount());
            pstmt.setDate(2, exp.getDate());
            pstmt.setString(3, exp.getDescription());
            pstmt.setString(4, exp.getType());
            pstmt.setString(5, exp.getDetails());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Insert failed, no rows affected.");
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    exp.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateExpense(Expense exp) throws SQLException {
        if (exp.getId() <= 0) throw new SQLException("Expense ID is invalid for update.");
        String sql = "UPDATE expenses SET amount=?, date=?, description=?, type=?, details=? WHERE id=?";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, exp.getAmount());
            pstmt.setDate(2, exp.getDate());
            pstmt.setString(3, exp.getDescription());
            pstmt.setString(4, exp.getType());
            pstmt.setString(5, exp.getDetails());
            pstmt.setInt(6, exp.getId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Update failed, no rows affected.");
        }
    }

    public void deleteExpense(int expenseId) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id=?";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Delete failed, no rows affected.");
        }
    }

    public List<Expense> getAllExpenses() throws SQLException {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses ORDER BY date DESC";
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                double amount = rs.getDouble("amount");
                java.sql.Date date = rs.getDate("date");
                String description = rs.getString("description");
                String type = rs.getString("type");
                String details = rs.getString("details");

                Expense exp;
                switch(type) {
                    case "Food":
                        exp = new FoodExpense(id, amount, date, description, details);
                        break;
                    case "Travel":
                        exp = new TravelExpense(id, amount, date, description, "", "");
                        exp.setDetailsFromString(details);
                        break;
                    case "Utility":
                        exp = new UtilityExpense(id, amount, date, description, details);
                        break;
                    default:
                        exp = new FoodExpense(id, amount, date, description, details);
                }
                expenses.add(exp);
            }
        }
        return expenses;
    }
}

class ExpenseManager {
    private final ExpenseDAO expenseDAO;

    public ExpenseManager() {
        expenseDAO = new ExpenseDAO();
    }

    public void addExpense(Expense expense) throws SQLException {
        expenseDAO.insertExpense(expense);
    }

    public void updateExpense(Expense expense) throws SQLException {
        expenseDAO.updateExpense(expense);
    }

    public void deleteExpense(int expenseId) throws SQLException {
        expenseDAO.deleteExpense(expenseId);
    }

    public List<Expense> getAllExpenses() throws SQLException {
        return expenseDAO.getAllExpenses();
    }

    public double getTotalExpenses(List<Expense> expenses) {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }
}

public class Expensetracker extends JFrame {
    private final ExpenseManager manager;
    private List<Expense> currentExpenses;

    private JComboBox<String> expenseTypeCombo;
    private JTextField amountField, descriptionField, dateField;
    private JTextField restaurantField, destinationField, transportField, utilityTypeField;
    private JButton addButton, updateButton, deleteButton, clearButton;

    private JTable expenseTable;
    private DefaultTableModel tableModel;

    private JLabel totalLabel;

    private int selectedRow = -1;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public Expensetracker() {
        manager = new ExpenseManager();
        currentExpenses = new ArrayList<>();
        setTitle("Expense Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        initComponents();
        layoutComponents();
        registerListeners();
        loadExpenses();
    }

    private void initComponents() {
        expenseTypeCombo = new JComboBox<>(new String[]{"Food", "Travel", "Utility"});
        amountField = new JTextField();
        descriptionField = new JTextField();
        dateField = new JTextField(dateFormat.format(new java.sql.Date(System.currentTimeMillis())));
        dateField.setToolTipText("Date format: yyyy-MM-dd");

        restaurantField = new JTextField();
        destinationField = new JTextField();
        transportField = new JTextField();
        utilityTypeField = new JTextField();

        addButton = new JButton("Add Expense");
        updateButton = new JButton("Update Expense");
        updateButton.setEnabled(false);
        deleteButton = new JButton("Delete Expense");
        deleteButton.setEnabled(false);
        clearButton = new JButton("Clear Fields");

        String[] columns = {"Type", "Amount", "Date", "Description", "Details"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        expenseTable = new JTable(tableModel);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expenseTable.getTableHeader().setReorderingAllowed(false);
        expenseTable.setAutoCreateRowSorter(true);

        totalLabel = new JLabel("Total Expenses: $0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
    }

    private void layoutComponents() {
        JPanel formPanel = new JPanel();
        formPanel.setBorder(BorderFactory.createTitledBorder("Add / Edit Expense"));
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Expense Type:"), gbc);
        gbc.gridx = 1;
        expenseTypeCombo.setToolTipText("Select the type of expense");
        expenseTypeCombo.setPreferredSize(new Dimension(150, 24));
        formPanel.add(expenseTypeCombo, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 3;
        formPanel.add(amountField, gbc);

        gbc.gridx = 4;
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 5;
        formPanel.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 5;
        formPanel.add(descriptionField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 6;
        formPanel.add(createDynamicDetailsPanel(), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 6;
        formPanel.add(createButtonPanel(), gbc);
        gbc.gridwidth = 1;

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Expenses"));
        tablePanel.add(new JScrollPane(expenseTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(totalLabel);

        add(formPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private JPanel createDynamicDetailsPanel() {
        CardLayout cardLayout = new CardLayout();
        JPanel detailPanel = new JPanel(cardLayout);
        detailPanel.setName("DynamicDetailsPanel");

        JPanel foodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        foodPanel.add(new JLabel("Restaurant:"));
        restaurantField.setPreferredSize(new Dimension(200, 24));
        foodPanel.add(restaurantField);

        JPanel travelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        travelPanel.add(new JLabel("Destination:"));
        destinationField.setPreferredSize(new Dimension(130, 24));
        travelPanel.add(destinationField);
        travelPanel.add(Box.createHorizontalStrut(10));
        travelPanel.add(new JLabel("Transport Mode:"));
        transportField.setPreferredSize(new Dimension(130, 24));
        travelPanel.add(transportField);

        JPanel utilityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        utilityPanel.add(new JLabel("Utility Type:"));
        utilityTypeField.setPreferredSize(new Dimension(300, 24));
        utilityPanel.add(utilityTypeField);

        detailPanel.add(foodPanel, "Food");
        detailPanel.add(travelPanel, "Travel");
        detailPanel.add(utilityPanel, "Utility");

        expenseTypeCombo.addActionListener(e -> {
            cardLayout.show(detailPanel, (String) expenseTypeCombo.getSelectedItem());
        });

        return detailPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        return buttonPanel;
    }

    private void registerListeners() {
        addButton.addActionListener(e -> onAddExpense());
        updateButton.addActionListener(e -> onUpdateExpense());
        deleteButton.addActionListener(e -> onDeleteExpense());
        clearButton.addActionListener(e -> clearAllFields());

        expenseTable.getSelectionModel().addListSelectionListener(e -> onTableSelectionChanged(e));
    }

    private void loadExpenses() {
        try {
            currentExpenses = manager.getAllExpenses();
            refreshTable();
        } catch (SQLException ex) {
            showError("Failed to load expenses from database: " + ex.getMessage());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Expense exp : currentExpenses) {
            tableModel.addRow(new Object[]{
                    exp.getType(),
                    String.format("%.2f", exp.getAmount()),
                    dateFormat.format(exp.getDate()),
                    exp.getDescription() == null || exp.getDescription().isEmpty() ? "-" : exp.getDescription(),
                    exp.getDetails()
            });
        }
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        double total = manager.getTotalExpenses(currentExpenses);
        totalLabel.setText(String.format("Total Expenses: $%,.2f", total));
    }

    private void onAddExpense() {
        try {
            Expense newExpense = createExpenseFromForm();
            if(newExpense == null) return;
            manager.addExpense(newExpense);
            currentExpenses.add(0, newExpense);
            refreshTable();
            clearAllFields();
            showMessage("Expense added successfully.");
        } catch (SQLException ex) {
            showError("Failed to add expense: " + ex.getMessage());
        }
    }

    private void onUpdateExpense() {
        if (selectedRow < 0) {
            showError("Select an expense from the table to update.");
            return;
        }
        try {
            Expense updatedExpense = createExpenseFromForm();
            if(updatedExpense == null) return;
            Expense original = currentExpenses.get(selectedRow);
            updatedExpense.setId(original.getId());
            manager.updateExpense(updatedExpense);
            currentExpenses.set(selectedRow, updatedExpense);
            refreshTable();
            clearSelection();
            clearAllFields();
            showMessage("Expense updated successfully.");
        } catch (SQLException ex) {
            showError("Failed to update expense: " + ex.getMessage());
        }
    }

    private void onDeleteExpense() {
        if (selectedRow < 0) {
            showError("Select an expense from the table to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the selected expense?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Expense toDelete = currentExpenses.get(selectedRow);
                manager.deleteExpense(toDelete.getId());
                currentExpenses.remove(selectedRow);
                refreshTable();
                clearSelection();
                clearAllFields();
                showMessage("Expense deleted successfully.");
            } catch (SQLException ex) {
                showError("Failed to delete expense: " + ex.getMessage());
            }
        }
    }

    private void onTableSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            selectedRow = expenseTable.getSelectedRow();
            if (selectedRow >= 0) {
                selectedRow = expenseTable.convertRowIndexToModel(selectedRow);
                Expense expense = currentExpenses.get(selectedRow);
                fillFormWithExpense(expense);
                addButton.setEnabled(false);
                updateButton.setEnabled(true);
                deleteButton.setEnabled(true);
            } else {
                clearSelection();
                clearAllFields();
            }
        }
    }

    private void clearSelection() {
        expenseTable.clearSelection();
        selectedRow = -1;
        addButton.setEnabled(true);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void clearAllFields() {
        amountField.setText("");
        descriptionField.setText("");
        dateField.setText(dateFormat.format(new java.sql.Date(System.currentTimeMillis())));
        restaurantField.setText("");
        destinationField.setText("");
        transportField.setText("");
        utilityTypeField.setText("");
        expenseTypeCombo.setSelectedIndex(0);
        clearSelection();
    }

    private Expense createExpenseFromForm() {
        String type = (String) expenseTypeCombo.getSelectedItem();
        String amountText = amountField.getText().trim();
        String dateText = dateField.getText().trim();
        String description = descriptionField.getText().trim();

        if (amountText.isEmpty()) {
            showError("Amount is required.");
            amountField.requestFocus();
            return null;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Amount must be a positive number.");
            amountField.requestFocus();
            return null;
        }

        java.sql.Date date;
        try {
            dateFormat.setLenient(false);
            java.util.Date parsedDate = dateFormat.parse(dateText);
            date = new java.sql.Date(parsedDate.getTime());
        } catch (ParseException ex) {
            showError("Date must be in yyyy-MM-dd format.");
            dateField.requestFocus();
            return null;
        }

        switch (type) {
            case "Food":
                String restaurant = restaurantField.getText().trim();
                if (restaurant.isEmpty()) {
                    showError("Restaurant is required for Food expenses.");
                    restaurantField.requestFocus();
                    return null;
                }
                return new FoodExpense(amount, date, description, restaurant);
            case "Travel":
                String destination = destinationField.getText().trim();
                String transport = transportField.getText().trim();
                if (destination.isEmpty()) {
                    showError("Destination is required for Travel expenses.");
                    destinationField.requestFocus();
                    return null;
                }
                if (transport.isEmpty()) {
                    showError("Transport Mode is required for Travel expenses.");
                    transportField.requestFocus();
                    return null;
                }
                return new TravelExpense(amount, date, description, destination, transport);
            case "Utility":
                String utilityType = utilityTypeField.getText().trim();
                if (utilityType.isEmpty()) {
                    showError("Utility Type is required for Utility expenses.");
                    utilityTypeField.requestFocus();
                    return null;
                }
                return new UtilityExpense(amount, date, description, utilityType);
            default:
                showError("Unknown expense type.");
                return null;
        }
    }

    private void fillFormWithExpense(Expense exp) {
        amountField.setText(String.valueOf(exp.getAmount()));
        descriptionField.setText(exp.getDescription() != null ? exp.getDescription() : "");
        dateField.setText(dateFormat.format(exp.getDate()));
        expenseTypeCombo.setSelectedItem(exp.getType());

        if (exp instanceof FoodExpense) {
            restaurantField.setText(((FoodExpense) exp).getRestaurant());
        } else {
            restaurantField.setText("");
        }

        if (exp instanceof TravelExpense) {
            destinationField.setText(((TravelExpense) exp).getDestination());
            transportField.setText(((TravelExpense) exp).getTransportMode());
        } else {
            destinationField.setText("");
            transportField.setText("");
        }

        if (exp instanceof UtilityExpense) {
            utilityTypeField.setText(((UtilityExpense) exp).getUtilityType());
        } else {
            utilityTypeField.setText("");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            Expensetracker tracker = new Expensetracker();
            tracker.setVisible(true);
        });
    }
}

