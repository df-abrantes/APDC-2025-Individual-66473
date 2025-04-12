import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

class UserProfilePage extends StatefulWidget {
  final String username;
  const UserProfilePage({super.key, required this.username});

  @override
  State<UserProfilePage> createState() => _UserProfilePageState();
}

class _UserProfilePageState extends State<UserProfilePage> {
  Map<String, dynamic> userData = {};
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _fetchUserData();
  }

  Future<void> _fetchUserData() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');

    final response = await http.post(
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/listusers'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'token': token}),
    );

    if (response.statusCode == 200) {
      final List<dynamic> allUsers = jsonDecode(utf8.decode(response.bodyBytes));
      // Encontrar o user com o username que queremos
      final user = allUsers.firstWhere(
        (u) => u['username'] == widget.username,
        orElse: () => {},
      );

      setState(() {
        userData = Map<String, dynamic>.from(user);
        loading = false;
      });
    } else {
      print('Erro: ${response.statusCode}');
      setState(() {
        loading = false;
      });
    }
  }

  Widget _buildTextField(String label, String value) {
    return TextFormField(
      initialValue: value.isEmpty || value == "NOT DEFINED" ? "NOT DEFINED" : value,
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: Colors.black87),
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
      enabled: false,
      style: const TextStyle(fontSize: 14),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F1D37),
        title: const Text('Perfil de Utilizador', style: TextStyle(color: Colors.white)),
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    const Text(
                      'Perfil do Utilizador',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 16),
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
                              _buildTextField('✉️ Email', userData['email'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('👤 Username', userData['username'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('📛 Nome Completo', userData['nome_completo'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('💳 Cartão de Cidadão', userData['numero_cartao_cidadao'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('🔢 NIF Utilizador', userData['nif_utilizador'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('💼 Função', userData['funcao'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('📞 Telefone', userData['telefone'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('🏠 Morada', userData['morada'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('🏢 Entidade Empregadora', userData['entidade_empregadora'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('🧾 NIF da Entidade', userData['nif_entidade_empregadora'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('👥 Perfil', userData['perfil'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('🛡️ Role', userData['role'] ?? ''),
                              const SizedBox(height: 12),
                              _buildTextField('🔒 Estado da Conta', userData['estado_conta'] ?? ''),
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
