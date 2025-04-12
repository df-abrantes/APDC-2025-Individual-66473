import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

import 'login_page.dart';
import 'change_password_page.dart';
import 'remove_account_page.dart';
import 'change_role_page.dart';
import 'change_attributes_page.dart';
import 'change_state_page.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  String userRole = '';
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    fetchUserRole();
  }

  Future<void> fetchUserRole() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');

    if (token == null) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginPage()),
      );
      return;
    }

    final response = await http.post(
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/profile'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'token': token}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      setState(() {
        userRole = data['role'];
        isLoading = false;
      });
    } else {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginPage()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Scaffold(
        backgroundColor: Color(0xFF0F1D37),
        body: Center(
          child: CircularProgressIndicator(color: Colors.white),
        ),
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F1D37),
        elevation: 0,
        title: const Text(''),
        centerTitle: true,
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: Center(
        child: Container(
          width: 320,
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Settings',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF0F1D37),
                ),
              ),
              const SizedBox(height: 24),
              _buildSettingsButton(
                context,
                'Change Password',
                () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => const ChangePasswordPage(),
                    ),
                  );
                },
              ),
              const SizedBox(height: 12),
              _buildSettingsButton(
                context,
                'Change Attributes',
                () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => const ChangeAttributesPage(),
                    ),
                  );
                },
              ),
              const SizedBox(height: 12),
              if (userRole != 'ENDUSER') ...[
                _buildSettingsButton(
                  context,
                  'Change State',
                  () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const ChangeStatePage(),
                      ),
                    );
                  },
                ),
                const SizedBox(height: 12),
                _buildSettingsButton(
                  context,
                  'Change Role',
                  () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const ChangeRolePage(),
                      ),
                    );
                  },
                ),
                const SizedBox(height: 12),
                _buildSettingsButton(
                  context,
                  'Remove Account',
                  () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const RemoveAccountPage(),
                      ),
                    );
                  },
                  color: Colors.red,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSettingsButton(
    BuildContext context,
    String label,
    VoidCallback onPressed, {
    Color color = const Color(0xFF00B49F),
  }) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: onPressed,
        child: Center(
          child: Text(
            label,
            style: const TextStyle(fontSize: 18),
          ),
        ),
        style: ElevatedButton.styleFrom(
          backgroundColor: color,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 24),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}
