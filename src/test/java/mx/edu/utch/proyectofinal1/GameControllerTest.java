package mx.edu.utch.proyectofinal1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    @Test
    void testSumarPuntaje() {
        GameController controller = new GameController();
        int resultado = controller.sumarPuntaje(5, 3);
        assertEquals(8, resultado, "El puntaje total debería ser 8");
    }
}
