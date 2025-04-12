import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;

import 'login_page.dart';
import 'profile_page.dart';
import 'users_page.dart';
import 'settings_page.dart';

class MainPage extends StatelessWidget {
  const MainPage({super.key});

  Future<void> _logout(BuildContext context) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    await prefs.clear();

    try {
      await http.post(
        Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/logout'),
        headers: {'Content-Type': 'application/json'},
        body: '{"token": "$token"}',
      );
    } catch (e) {
      debugPrint("Erro ao terminar sessão remotamente: $e");
    }

    if (context.mounted) {
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (_) => const LoginPage()),
        (route) => false,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
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
                'Bem-vindo',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF0F1D37),
                ),
              ),
              const SizedBox(height: 24),
              _buildMainButton(context, 'Profile', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const ProfilePage()),
                );
              }),
              const SizedBox(height: 12),
              _buildMainButton(context, 'Users', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const UsersPage()),
                );
              }),
              const SizedBox(height: 12),
              _buildMainButton(context, 'Settings', () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const SettingsPage()),
                );
              }),
              const SizedBox(height: 12),
              _buildMainButton(
                context,
                'Logout',
                () => _logout(context),
                color: Colors.red,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMainButton(
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
