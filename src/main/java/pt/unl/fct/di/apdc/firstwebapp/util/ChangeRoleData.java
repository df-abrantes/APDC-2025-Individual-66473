package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeRoleData {
    public String token;
    public String email;
    public String newRole;

    public ChangeRoleData() {}

    public ChangeRoleData(String token, String email, String newRole) {
        this.token = token;
        this.email = email;
        this.newRole = newRole;
    }

    public boolean isValid() {
        return token != null && !token.isEmpty()
            && email != null && !email.isEmpty()
            && newRole != null && !newRole.isEmpty();
    }
}
