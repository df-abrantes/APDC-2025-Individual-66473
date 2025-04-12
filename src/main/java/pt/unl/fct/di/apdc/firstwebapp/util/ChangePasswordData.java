package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangePasswordData {
    public String token;
    public String currentPassword;
    public String newPassword;
    public String confirmPassword;

    public ChangePasswordData() {}

    public ChangePasswordData(String token, String currentPassword, String newPassword, String confirmPassword) {
        this.token = token;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}
