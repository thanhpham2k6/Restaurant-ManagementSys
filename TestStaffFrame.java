public class TestStaffFrame {
    public static void main(String[] args) {
        try {
            new com.nhom13.restaurant.managementsys.view.StaffMainFrame().setVisible(true);
            System.out.println("StaffMainFrame opened successfully.");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
