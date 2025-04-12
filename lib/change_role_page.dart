import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class ChangeRolePage extends StatefulWidget {
  const ChangeRolePage({super.key});

  @override
  State<ChangeRolePage> createState() => _ChangeRolePageState();
}

class _ChangeRolePageState extends State<ChangeRolePage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  String? _selectedRole;
  bool _isLoading = false;

  final List<String> _roles = ['ENDUSER', 'BACKOFFICE', 'ADMIN', 'PARTNER'];

  Future<void> _submitChangeRole() async {
    if (!_formKey.currentState!.validate() || _selectedRole == null) return;

    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    if (token == null) {
      _showSnackBar('Token não encontrado.', Colors.red);
      return;
    }

    setState(() {
      _isLoading = true;
    });

    final response = await http.post(
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/changerole'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'token': token,
        'email': _emailController.text,
        'newRole': _selectedRole,
      }),
    );

    setState(() {
      _isLoading = false;
    });

    if (response.statusCode == 200) {
      _showSnackBar('Role alterado com sucesso!', Colors.green);
    } else {
      String errorMessage = 'Erro ao alterar o role.';
      try {
        final errorResponse = jsonDecode(utf8.decode(response.bodyBytes));
        if (errorResponse is Map && errorResponse.containsKey('message')) {
          errorMessage = errorResponse['message'];
        }
      } catch (e) {
        print('Erro ao tentar ler a mensagem do erro: $e');
      }
      _showSnackBar(errorMessage, Colors.red);
    }
  }

  void _showSnackBar(String message, Color backgroundColor) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: backgroundColor,
      ),
    );
  }

  Widget _buildEmailField() {
    return TextFormField(
      controller: _emailController,
      decoration: InputDecoration(
        labelText: '📧 Email da Conta',
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        labelStyle: const TextStyle(color: Colors.black87),
      ),
      validator: (value) =>
          value == null || value.isEmpty ? 'Campo obrigatório' : null,
    );
  }

  Widget _buildRoleDropdown() {
    return DropdownButtonFormField<String>(
      value: _selectedRole,
      items: _roles
          .map((role) => DropdownMenuItem(value: role, child: Text(role)))
          .toList(),
      onChanged: (value) => setState(() => _selectedRole = value),
      decoration: InputDecoration(
        labelText: '🛡️ Novo Role',
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        labelStyle: const TextStyle(color: Colors.black87),
      ),
      validator: (value) =>
          value == null ? 'Por favor, selecione um role' : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: Alignment.topLeft,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                  onPressed: () => Navigator.pop(context),
                ),
              ),
              const Text(
                'Alterar Role',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 16),
              Center(
                child: Container(
                  width: 500,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        _buildEmailField(),
                        const SizedBox(height: 12),
                        _buildRoleDropdown(),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _submitChangeRole,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFF00B49F),
                              padding: const EdgeInsets.symmetric(vertical: 16),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            child: _isLoading
                                ? const CircularProgressIndicator(
                                    color: Colors.white)
                                : const Text(
                                    'Confirmar Alteração',
                                    style: TextStyle(
                                        fontSize: 16, color: Colors.white),
                                  ),
                          ),
                        ),
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
