package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeStateData {
    public String token; // Token de autenticação
    public String email; // Email da conta alvo
    public String newState; // Novo estado da conta (ATIVADA, SUSPENSA ou DESATIVADA)

    // Construtor padrão
    public ChangeStateData() {}

    // Validação compacta
    public boolean isValid() {
        return token != null && !token.isEmpty() &&
               email != null && !email.isEmpty() &&
               newState != null && !newState.isEmpty() &&
               (newState.equals("ATIVADA") || newState.equals("SUSPENSA") || newState.equals("DESATIVADA"));
    }
}
