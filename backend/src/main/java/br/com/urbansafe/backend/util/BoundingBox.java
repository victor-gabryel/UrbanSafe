package br.com.urbansafe.backend.util;

import br.com.urbansafe.backend.exception.ApiException;

/**
 * Área retangular do mapa. Formato do parâmetro:
 * {@code minLon,minLat,maxLon,maxLat} (graus decimais,
 * ordem PostGIS: longitude antes de latitude). Não há suporte a áreas que
 * cruzam o antimeridiano (180°).
 */
public record BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {

    public static BoundingBox parse(String texto, double maxLadoGraus) {
        if (texto == null || texto.isBlank()) {
            throw ApiException.badRequest("O parâmetro bbox é obrigatório (minLon,minLat,maxLon,maxLat).");
        }

        String[] partes = texto.split(",", -1);
        if (partes.length != 4) {
            throw ApiException.badRequest("bbox inválido: use minLon,minLat,maxLon,maxLat.");
        }

        double[] v = new double[4];
        for (int i = 0; i < 4; i++) {
            try {
                v[i] = Double.parseDouble(partes[i].trim());
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("bbox inválido: valores devem ser numéricos.");
            }
            if (!Double.isFinite(v[i])) {
                throw ApiException.badRequest("bbox inválido: valores devem ser números finitos.");
            }
        }

        BoundingBox box = new BoundingBox(v[0], v[1], v[2], v[3]);
        box.validar(maxLadoGraus);
        return box;
    }

    public void validar(double maxLadoGraus) {
        if (minLat < -90 || minLat > 90 || maxLat < -90 || maxLat > 90) {
            throw ApiException.badRequest("bbox inválido: a latitude deve estar entre -90 e 90.");
        }
        if (minLon < -180 || minLon > 180 || maxLon < -180 || maxLon > 180) {
            throw ApiException.badRequest("bbox inválido: a longitude deve estar entre -180 e 180.");
        }
        if (minLon >= maxLon || minLat >= maxLat) {
            throw ApiException.badRequest("bbox inválido: minLon/minLat devem ser menores que maxLon/maxLat.");
        }
        if (maxLon - minLon > maxLadoGraus || maxLat - minLat > maxLadoGraus) {
            throw ApiException.badRequest(
                    "bbox muito grande: cada lado pode ter no máximo " + maxLadoGraus + " graus.");
        }
    }
}
