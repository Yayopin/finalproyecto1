package mx.edu.utch.proyectofinal1;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameController {

    @FXML private TextField txtNombre;
    @FXML private Label lblNivel;
    @FXML private Label lblEstado;

    @FXML private Button btnRojo;
    @FXML private Button btnAzul;
    @FXML private Button btnVerde;
    @FXML private Button btnAmarillo;


    @FXML private Button btnIniciar;
    @FXML private Button btnRecords;

    private final Random random = new Random();
    private final List<Integer> secuencia = new ArrayList<>();
    private final List<Integer> entrada = new ArrayList<>();

    private boolean jugando = false;
    private boolean mostrando = false;


    private Integer playerId = null;
    private String playerName = null;

    @FXML
    private void initialize() {
        setHabilitarColores(false);
        lblNivel.setText("0");
        lblEstado.setText("Pon tu nombre y picale Iniciar");
        setUIPlaying(false);
    }


    private void setUIPlaying(boolean playing) {

        if (btnIniciar != null) { btnIniciar.setVisible(!playing); btnIniciar.setManaged(!playing); }
        if (btnRecords != null) { btnRecords.setVisible(!playing); btnRecords.setManaged(!playing); }
        if (txtNombre != null) txtNombre.setDisable(playing);
    }

    private void setHabilitarColores(boolean value) {
        btnRojo.setDisable(!value);
        btnAzul.setDisable(!value);
        btnVerde.setDisable(!value);
        btnAmarillo.setDisable(!value);
    }


    @FXML
    private void onIniciar() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            lblEstado.setText("Pon tu nombre primero pibe");
            txtNombre.requestFocus();
            return;
        }

        try {
            playerName = nombre;
            playerId = Database.upsertPlayer(playerName);
            if (playerId == null || playerId <= 0) {
                lblEstado.setText("No pude registrar al jugador ");
                return;
            }
        } catch (Exception e) {
            lblEstado.setText("Error con la BD: " + e.getMessage());
            return;
        }

        reiniciarJuego();
        setUIPlaying(true);
        lblEstado.setText("Observa la secuencia");
        siguienteRonda();
    }

    @FXML
    private void onReiniciar() {
        reiniciarJuego();
        setUIPlaying(false);
        lblEstado.setText("Listo para iniciar");
    }

    @FXML
    private void onVerRecords() {
        String url = "jdbc:sqlite:data/simon.db";
        String sql = """
            SELECT p.name AS nombre, MAX(s.score) AS puntaje
            FROM players p
            JOIN scores  s ON s.player_id = p.id
            GROUP BY p.id, p.name
            ORDER BY puntaje DESC
            LIMIT 10
            """;

        StringBuilder sb = new StringBuilder("🏆 Top 10 jugadores\n\n");

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int pos = 1;
            while (rs.next()) {
                sb.append(pos++).append(". ")
                        .append(rs.getString("nombre"))
                        .append(" — ")
                        .append(rs.getInt("puntaje"))
                        .append("\n");
            }
            if (pos == 1) sb.append("Aún no hay récords. Juega una partida para generar datos.\n");

        } catch (SQLException e) {
            sb.append("Error al cargar récords: ").append(e.getMessage());
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Récords");
        a.setHeaderText("Top 10 jugadores");
        a.setContentText(sb.toString());
        a.showAndWait();
    }


    @FXML
    private void onColorClick(ActionEvent e) {
        if (!jugando || mostrando) return;

        int idx = -1;
        Object src = e.getSource();
        if (src == btnRojo) idx = 0;
        else if (src == btnAzul) idx = 1;
        else if (src == btnVerde) idx = 2;
        else if (src == btnAmarillo) idx = 3;

        if (idx == -1) return;

        flashButton(getButtonByIndex(idx), 160);
        entrada.add(idx);

        int pos = entrada.size() - 1;
        if (!entrada.get(pos).equals(secuencia.get(pos))) {
            gameOver();
            return;
        }

        if (entrada.size() == secuencia.size()) {
            lblEstado.setText("Buenisima, siguiente nivel");
            pausa(400, this::siguienteRonda);
        }
    }


    private void reiniciarJuego() {
        jugando = false;
        mostrando = false;
        secuencia.clear();
        entrada.clear();
        lblNivel.setText("0");
        setHabilitarColores(false);
    }

    private void siguienteRonda() {
        entrada.clear();
        secuencia.add(random.nextInt(4));
        lblNivel.setText(String.valueOf(secuencia.size()));
        mostrarSecuencia();
    }

    private void mostrarSecuencia() {
        mostrando = true;
        jugando = false;
        setHabilitarColores(false);
        lblEstado.setText("mira");

        Timeline timeline = new Timeline();
        int preDelay = 700;
        int flashMs  = 700;
        int gapMs    = 500;

        for (int i = 0; i < secuencia.size(); i++) {
            int idx = secuencia.get(i);
            Button b = getButtonByIndex(idx);
            double start = preDelay + i * (flashMs + gapMs);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(start),
                    ev -> flashButton(b, flashMs)));
        }

        timeline.setOnFinished(ev -> {
            mostrando = false;
            jugando = true;
            setHabilitarColores(true);
            lblEstado.setText("Vas");
        });

        timeline.play();
    }


    private void gameOver() {
        jugando = false;
        setHabilitarColores(false);
        int score = Math.max(0, secuencia.size() - 1);

        if (playerId != null && playerId > 0) {
            try {
                Database.saveScore(playerId, score);
                lblEstado.setText("Fallaste en el nivel " + secuencia.size() + ". Récord guardado para " + playerName + ".");
            } catch (Exception e) {
                lblEstado.setText("Fallaste en el nivel " + secuencia.size() + ". (No se pudo guardar el récord: " + e.getMessage() + ")");
            }
        } else {
            lblEstado.setText("Fallaste en el nivel " + secuencia.size() + ".");
        }

        setUIPlaying(false);
    }


    private Button getButtonByIndex(int idx) {
        return switch (idx) {
            case 0 -> btnRojo;
            case 1 -> btnAzul;
            case 2 -> btnVerde;
            case 3 -> btnAmarillo;
            default -> null;
        };
    }

    private void flashButton(Button b, int ms) {
        if (b == null) return;
        String original = b.getStyle();
        String highlight;

        if (b == btnRojo)      highlight = "-fx-background-color: #ff9a9a; -fx-background-radius: 12;";
        else if (b == btnAzul) highlight = "-fx-background-color: #9ac8ff; -fx-background-radius: 12;";
        else if (b == btnVerde)highlight = "-fx-background-color: #9affb2; -fx-background-radius: 12;";
        else                   highlight = "-fx-background-color: #fff59a; -fx-background-radius: 12;";

        b.setStyle(highlight);
        b.setScaleX(1.10);
        b.setScaleY(1.10);

        pausa(ms, () -> {
            b.setStyle(original);
            b.setScaleX(1.0);
            b.setScaleY(1.0);
        });
    }

    private void pausa(int ms, Runnable after) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> after.run());
        p.play();
    }
    public int sumarPuntaje(int puntajeActual, int puntosGanados) {
        return puntajeActual + puntosGanados;
    }

}
