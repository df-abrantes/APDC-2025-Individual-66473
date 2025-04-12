package pt.unl.fct.di.apdc.firstwebapp.util;

public class RemoveUserData {

    public String token;
	public String email;
	
	public RemoveUserData() {}
	
	public RemoveUserData(String token, String email) {
		this.token = token;
		this.email = email;
	}
}
