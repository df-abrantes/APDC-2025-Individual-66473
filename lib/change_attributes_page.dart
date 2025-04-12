import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

class ChangeAttributesPage extends StatefulWidget {
  const ChangeAttributesPage({super.key});

  @override
  State<ChangeAttributesPage> createState() => _ChangeAttributesPageState();
}

class _ChangeAttributesPageState extends State<ChangeAttributesPage> {
  bool loading = true;
  String userRole = "";

  // Controllers para os campos
  final TextEditingController emailController = TextEditingController();
  final TextEditingController newEmailController = TextEditingController();
  final TextEditingController nomeCompletoController = TextEditingController();
  final TextEditingController telefoneController = TextEditingController();
  final TextEditingController usernameController = TextEditingController();
  final TextEditingController nifUtilizadorController = TextEditingController();
  final TextEditingController nifEntidadeEmpController = TextEditingController();
  final TextEditingController moradaController = TextEditingController();
  final TextEditingController funcaoController = TextEditingController();
  final TextEditingController entidadeEmpController = TextEditingController();
  final TextEditingController numeroCartaoController = TextEditingController();

  // Variável para o dropdown do perfil
  String? selectedPerfil;

  @override
  void initState() {
    super.initState();
    _fetchProfile();
  }

  Future<void> _fetchProfile() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');

    final response = await http.post(
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/profile'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'token': token}),
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data =
          jsonDecode(utf8.decode(response.bodyBytes));
      setState(() {
        emailController.text = data['email'] ?? "";
        newEmailController.text = data['newEmail'] ?? "";
        nomeCompletoController.text = data['nome_completo'] ?? "";
        // Para perfil, usamos o dropdown; se não definido, padrão "público"
        selectedPerfil = data['perfil'] ?? "público";
        telefoneController.text = data['telefone'] ?? "";
        usernameController.text = data['username'] ?? "";
        nifUtilizadorController.text = data['nif_utilizador'] ?? "";
        nifEntidadeEmpController.text =
            data['nif_entidade_empregadora'] ?? "";
        moradaController.text = data['morada'] ?? "";
        funcaoController.text = data['funcao'] ?? "";
        entidadeEmpController.text = data['entidade_empregadora'] ?? "";
        numeroCartaoController.text = data['numero_cartao_cidadao'] ?? "";
        userRole = data['role'] ?? "";
        loading = false;
      });
    } else {
      setState(() {
        loading = false;
      });
    }
  }

  // Widget para construir campos de texto
  Widget _buildTextField(
      String label, TextEditingController controller, bool enabled) {
    return TextFormField(
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: Colors.black87),
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
      enabled: enabled,
      style: const TextStyle(fontSize: 14),
    );
  }

  // Widget para o dropdown do campo "perfil"
  Widget _buildPerfilDropdown({bool enabled = false}) {
    return DropdownButtonFormField<String>(
      value: selectedPerfil,
      items: const [
        DropdownMenuItem(value: "público", child: Text("público")),
        DropdownMenuItem(value: "privado", child: Text("privado")),
      ],
      onChanged: enabled
          ? (value) {
              setState(() {
                selectedPerfil = value;
              });
            }
          : null,
      decoration: InputDecoration(
        labelText: "👥 Perfil",
        labelStyle: const TextStyle(color: Colors.black87),
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
      style: const TextStyle(fontSize: 14),
    );
  }

  // Método para enviar as alterações
  Future<void> _submitChanges() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');

    final Map<String, dynamic> payload = {
      'token': token,
      'email': emailController.text,
      'newEmail': newEmailController.text,
      'nome_completo': nomeCompletoController.text,
      'perfil': selectedPerfil,
      'telefone': telefoneController.text,
      'username': usernameController.text,
      'nif_utilizador': nifUtilizadorController.text,
      'nif_entidade_empregadora': nifEntidadeEmpController.text,
      'morada': moradaController.text,
      'funcao': funcaoController.text,
      'entidade_empregadora': entidadeEmpController.text,
      'numero_cartao_cidadao': numeroCartaoController.text,
    };

    final response = await http.post(
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/changeattributes'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(payload),
    );

    if (response.statusCode == 200) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text("Atributos atualizados com sucesso!"),
            backgroundColor: Color(0xFF00B49F)),
      );
    } else {
      String errorMessage = 'Erro desconhecido';
      try {
        final errorResponse = jsonDecode(utf8.decode(response.bodyBytes));
        if (errorResponse is Map && errorResponse.containsKey('message')) {
          errorMessage = errorResponse['message'];
        }
      } catch (e) {
        print('Erro ao tentar ler a mensagem do erro: $e');
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text("Erro ao atualizar atributos: $errorMessage"),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    // Regras de edição de acordo com o role:
    // ADMIN: Todos os campos editáveis.
    // BACKOFFICE: Não editáveis: newEmail e username.
    // ENDUSER: Não editáveis: newEmail, username, e nome_completo.
    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F1D37),
        title: const Text(
          'Alterar Atributos',
          style:
              TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
        ),
        centerTitle: true,
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: SingleChildScrollView(
                          child: Column(
                            children: [
                              if (userRole == 'ADMIN') ...[
                                _buildTextField('✉️ Email', emailController, true),
                                const SizedBox(height: 12),
                                _buildTextField('👤 Username', usernameController, true),
                                const SizedBox(height: 12),
                                _buildTextField('📛 Nome Completo', nomeCompletoController, true),
                                const SizedBox(height: 12),
                                _buildTextField('💳 Cartão de Cidadão', numeroCartaoController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🔢 NIF Utilizador', nifUtilizadorController, true),
                                const SizedBox(height: 12),
                                _buildTextField('💼 Função', funcaoController, true),
                                const SizedBox(height: 12),
                                _buildTextField('📞 Telefone', telefoneController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🏠 Morada', moradaController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🏢 Entidade Empregadora', entidadeEmpController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🧾 NIF da Entidade', nifEntidadeEmpController, true),
                                const SizedBox(height: 12),
                                _buildPerfilDropdown(enabled: true),
                                const SizedBox(height: 12),
                                // Campo Role (só leitura, se desejar mostrar o papel)
                                _buildTextField('📧 Novo Email', newEmailController, true),
                              ] else if (userRole == 'BACKOFFICE') ...[
                                _buildTextField('✉️ Email', emailController, true),
                                const SizedBox(height: 12),
                                _buildTextField('💼 Função', funcaoController, true),
                                const SizedBox(height: 12),
                                _buildTextField('📞 Telefone', telefoneController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🏠 Morada', moradaController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🏢 Entidade Empregadora', entidadeEmpController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🧾 NIF da Entidade', nifEntidadeEmpController, true),
                                const SizedBox(height: 12),
                                _buildPerfilDropdown(enabled: true),
                                const SizedBox(height: 12),
                                // newEmail e username não são editáveis para BACKOFFICE
                              ] else if (userRole == 'ENDUSER') ...[
                                _buildTextField('✉️ Email', emailController, true),
                                const SizedBox(height: 12),
                                _buildTextField('💼 Função', funcaoController, true),
                                const SizedBox(height: 12),
                                _buildTextField('📞 Telefone', telefoneController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🏠 Morada', moradaController, true),
                                const SizedBox(height: 12),
                                _buildTextField('🧾 NIF da Entidade', nifEntidadeEmpController, true),
                                const SizedBox(height: 12),
                                _buildPerfilDropdown(enabled: true),
                                const SizedBox(height: 12)
                                // newEmail, username e nome_completo não são editáveis para ENDUSER.
                              ],
                              const SizedBox(height: 24),
                              SizedBox(
                                width: double.infinity,
                                child: ElevatedButton(
                                  onPressed: _submitChanges,
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: const Color(0xFF00B49F),
                                    padding: const EdgeInsets.symmetric(vertical: 16),
                                    shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(12)),
                                  ),
                                  child: const Text(
                                    'Confirmar Alterações',
                                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                                  ),
                                ),
                              )
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
