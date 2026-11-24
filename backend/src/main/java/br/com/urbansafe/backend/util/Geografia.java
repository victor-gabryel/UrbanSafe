package br.com.urbansafe.backend.util;

/** Cálculos geográficos simples, sem dependências do Spring. */
public final class Geografia {

    private static final double RAIO_TERRA_METROS = 6_371_008.8;

    private Geografia() {
    }

    /** Distância em linha reta (haversine) entre dois pontos, em metros. */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double fi1 = Math.toRadians(lat1);
        double fi2 = Math.toRadians(lat2);
        double dFi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dFi / 2) * Math.sin(dFi / 2)
                + Math.cos(fi1) * Math.cos(fi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return 2 * RAIO_TERRA_METROS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
