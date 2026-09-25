import { StyleSheet, Text, View } from "react-native";

export default function HomeScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>UrbanSafe</Text>

      <Text style={styles.subtitle}>
        Segurança, proteção e informação para a comunidade.
      </Text>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Bem-vindo ao UrbanSafe</Text>

        <Text style={styles.cardText}>
          Aqui você poderá registrar e acompanhar ocorrências
          relacionadas à segurança urbana.
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 24,
    backgroundColor: "#f5f7fa",
  },

  title: {
    fontSize: 36,
    fontWeight: "bold",
    color: "#1f2937",
    marginBottom: 12,
  },

  subtitle: {
    fontSize: 16,
    textAlign: "center",
    color: "#6b7280",
    marginBottom: 30,
  },

  card: {
    width: "100%",
    padding: 24,
    borderRadius: 16,
    backgroundColor: "#ffffff",
  },

  cardTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#1f2937",
    marginBottom: 10,
  },

  cardText: {
    fontSize: 15,
    lineHeight: 22,
    color: "#6b7280",
  },
});