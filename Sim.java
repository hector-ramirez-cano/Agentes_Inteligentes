import java.time.LocalTime;
import java.util.Random;

class Sim {

	// valor de los sensores
	static float A1 = 0;                                    // Sensor de humedad (Interno) [0, 1] 
	static float A2 = 0;                                    // Sensor de temperatura °C
	static float A3 = 0;                                    // Sensor de CO2 en PPM
	static LocalTime A4 = LocalTime.parse("08:40:00");//  Reloj
	static float A5 = 0;                                    // Sensor de nivel de agua en el humidificador
	static float A6 = 0;                                    // Sensor de humedad (Externo) [0, 1]
	static boolean A7 = false;                              // Sensor de movimiento

	// estado de los actuadores
	static boolean B1 = false;                              // Abre ventanas
	static short   B2 = 0;                                  // Calefactor/Aire acondicionado
	static boolean B3 = false;                              // Humidificador
	static float   B4 = 0.0f;                               // Dimmer de Luz
	static boolean B5 = false;                              // Refill de agua de humidificador
	static boolean B6 = false;                              // Filtro de CO
	
	// variables definidas por el usuario
	static LocalTime inicioHorasActivas = LocalTime.parse("07:00:00");  // 7 AM
	static LocalTime finHorasActivas    = LocalTime.parse("21:00:00"); // 9 PM
	static float tempMin = 24; // °C
	static float tempMax = 28; // °C
	static float tempObj = tempMin + 0.5f*(tempMax - tempMin);
	static float nivelBrilloMin = 0.1f; // [0, 1]
	static float nivelBrilloMax = 0.8f; // [0, 1]
	static float humMin = 0.2f; // 20%
	static float humMax = 0.6f; // 60%
	static float humObj = humMax;

	static Random randEngine = new Random(LocalTime.now().getNano());

	public static void evaluarCondiciones() {
		// B4 - Dimmer de Luz
		if(!A7) { B4 = 0; } // si no hay nadie, apagamos la luz
		else if(A6 > 0.7) { // está lloviendo y nublado, incrementamos el brillo al máximo
			B4 = 1.0f;
		}
		else if((A4.isAfter(inicioHorasActivas) && A4.isBefore(finHorasActivas))) {
			B4 = nivelBrilloMax;
		}
		else if(A4.isAfter(finHorasActivas)) {
			B4 = nivelBrilloMin;
		}

		// B2 - Calefactor o Aire acondicionado
		if(!A7) { B2 = 0; } // si no hay nadie, apagamos la calefación
		else if(B2 != 0) { // si está encendido
			// y alcanzamos la temperatura objetivo
			if((Math.abs(A2 - tempObj) < 1)) { B2 = 0; }
		}
		else if(A2 <= tempMin) { B2 = 1; }   // si cae debajo de la temperatura mínima, lo encedemos a calentar
		else if(A2 >= tempMax) { B2  = -1; } // si sube de la temperatura máxima, lo encendemos a enfriar
		else { B2 = 0; } // fallback

		// B1 - Ventanas abiertas/cerradas
		if(!A7) { B1 = false; }
		else if(B1 && B2 == 0) {
			// si ya están abiertas
			if(Math.abs(A3 - 250) < 10) { B1 = false; }
		} else {
			// verificamos si deberíamos abrirlas
			B1 = !(A6 >= 0.7 || B2 == 1 || B2 == -1) && A3 > 300;
		}
		
		// B3 - Humidificador
		if(B3) { // si ya está encendido
			// si está cerca de la humedad objetivo
			if(Math.abs(A1 - humObj) < 0.05) { B3 = false; }
		} else {
			// verificamos si debería encenderse
			B3 = (A1 <= humMin || B2 == -1);
		}
		
		// B5 - Refill de agua de humidificador
		B5 = A5 < 0.1;

		// B6 - Filtro de CO2
		if(B6) {
			// si ya está encendido
			// y ya se alcanzó el objetivo de 150ppm
			if(Math.abs(A3 - 150) < 10) { B6 = false; }
		}
		else {
			// verificamos si debería encenderse
			B6 = (!(B1) && (B2 == 1 || B2 == -1)) || (A3 > 600);
		}
	}

	public static void actualizarEstado() {
		
		// A1 Humedad Interna, en función de si la ventana está abierta, si hay aire acondicionado, etc
		if(B2 == -1) { A1 -= 0.1; }
		if(B1) { // si la ventana está abierta
			// el 2% entra o sale, cada unidad de tiempo
			A1 += (A6 - A1) * 0.02;
		}
		if(B3) {  // si el humidificador está encendido
			A1 += 0.1;
		}
		A1 -= 0.005; // pérdida natural
		if(A1 > 1) { A1 = 1; }
		if(A1 < 0) { A1 = 0; }
		
		// A2 Temperatura, "Aleatorio" en función del tiempo
		// suponemos que el máximo es 33°C, el mínimo es 0°C
		A2 += 
			randEngine.nextFloat() *
			0.005 *
			(
				A4.isAfter(LocalTime.parse("07:00:00")) &&
				A4.isBefore(LocalTime.parse("19:00:00")) 
					? 1 : 0
			) * 
			(B1 ? 1 : 0.1) +
			(B2 == -1 ? -0.5 : (B2 == 1 ? 0.5 : 0));
		A2 -= 0.01; // pérdida natural
		if(A2 > 33) { A2 = 33; }
		if(A2 < 0) { A2 = (float)randEngine.nextGaussian() * 2.0f; }
		
		// A3 Sensor de CO2
		if(B1 && A3 > 200) { A3 -= 3; }
		if(B6 && A3 > 150) { A3 -= 2; }
		if(B2 == 1) { A3 += 2; }
		if(A7) { A3 += 1; }
		A3 += randEngine.nextGaussian(); // ruido natural

		// A4 Tiempo
		A4 = A4.plusMinutes(1);

		// A5 Agua del humidificador
		if(B5 && A5 < 0.95) { A5 += 0.02; }
		if(A5 < 0.1) { A5-= 0.01; }
		if(A5 < 0) { A5 = 0; }

		// A6 Humedad externa, "Aleatorio"
		A6 += randEngine.nextFloat() * 0.01 * (randEngine.nextBoolean() ? -1 : 1);
		if(A6 > 1) { A6 = 1.0f; }
		if(A6 < 0) { A6 = 0.0f; }

		// A7, controlado por el usuario, digamos que es "Aleatorio", con más probabilidad de que sí haya usuario
		A7 = randEngine.nextFloat() < 0.95;
		if(A4.isAfter(finHorasActivas) || A4.isBefore(inicioHorasActivas)) {
			A7 = false;
		}
	}

	public static String headersString() {

		return new String("Humedad(int)\tTemperatura\tCO2\tHora\tAg. Hum.\tHumedad(ext)\tMovimiento");
	}

	public static String estadoToString() { 
		StringBuilder result = new StringBuilder();

		result.append(String.format("%.6f", A1)).append("\t");
		result.append(String.format("%.6f", A2)).append("\t");
		result.append((int)A3).append("\t");
		result.append(A4).append("\t");
		result.append(String.format("%.6f", A5)).append("\t");
		result.append(String.format("%.6f", A6)).append("\t");
		result.append(A7).append("\t");

		result.append("\t|\t");

		result.append(B1 ? "1" : "0").append(" ");
		result.append(B2).append(" ");
		result.append(B3 ? "1" : "0").append(" ");
		result.append(B4).append(" ");
		result.append(B5 ? "1" : "0").append(" ");
		result.append(B6 ? "1" : "0").append(" ");

		return result.toString();
	}

	public static void main(String args[]) {
		// estado inicial
		
		A1 = randEngine.nextFloat();               // Sensor de humedad (Interno) [0, 1] 
		A2 = randEngine.nextFloat() * 22;          // Sensor de temperatura °C
		A3 = randEngine.nextFloat() * 2000;        // Sensor de CO2 en PPM
		A4 = LocalTime.parse("10:40:00");       //  Reloj
		A5 = randEngine.nextFloat();               // Sensor de nivel de agua en el humidificador
		A6 = randEngine.nextFloat();               // Sensor de humedad (Externo) [0, 1]
		A7 = randEngine.nextInt(1) == 0;    // Sensor de movimiento
		randEngine.setSeed(LocalTime.now().getNano()); // Reset RNG seed

		// dejamos correr la simulación
		System.out.println(headersString());
		int count = 0;
		while(count < 10000) {
			System.out.println(estadoToString());
			evaluarCondiciones();
			actualizarEstado();
			count++;
		}
	}

};