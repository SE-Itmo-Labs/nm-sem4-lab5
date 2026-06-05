package com.seifmolabs.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seifmolabs.exceptions.ValidationException;
import com.seifmolabs.objects.Point2D;
import com.seifmolabs.service.InterpolationService;

import io.javalin.Javalin;

public class ApproxApi {
    private static final ObjectMapper json = new ObjectMapper();
    private static final InterpolationService service = new InterpolationService();

    public static void register(Javalin app) {

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ctx.status(500).json(ApiResponse.error("Внутренняя ошибка сервера: " + msg));
        });

        // 1. CORS
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type");
        });
        app.options("/*", ctx -> ctx.status(200));

        // 2. Обработка ошибок
        app.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(400).json(ApiResponse.error(e.getMessage()));
        });
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(ApiResponse.error("Внутренняя ошибка сервера: " + e.getMessage()));
        });

        // 3. POST /api/calculate
        app.post("/api/calculate", ctx -> {

            CalculateRequest req = ctx.bodyAsClass(CalculateRequest.class);
            if (req.points == null || req.points.length < 2) throw new ValidationException("Нужно минимум 2 точки");
            if (req.points.length > 40) throw new ValidationException("Слишком много точек (максимум 40)");

            List<Point2D> points = new ArrayList<>();

            for (double[] pair : req.points) {
                points.add(new Point2D(pair[0], pair[1]));
            }

            points.sort(Comparator.comparingDouble(p -> p.x));

            double targetX = req.targetX != null ? req.targetX : points.get(0).x;

            boolean isEquidistant = service.isEquidistant(points, points.size());

            double[][] diffTable = null;

            if (isEquidistant) {
                diffTable = service.finiteDifferences(points, points.size());
            } else {
                diffTable = service.dividedDifferences(points, points.size());
            }

            List<Map<String, Object>> results = new ArrayList<>();

            if (req.funcType != null) {

                Function<Double, Double> origFunc = null;

                if (req.funcType == 1) origFunc = Math::cos;
                else if (req.funcType == 2) origFunc = x -> Math.pow(x, 3) - 4 * Math.pow(x, 2) + 6 * x - 2.1;
                else if (req.funcType == 3) origFunc = x -> 0.5 * Math.exp(x);

                if (origFunc != null) {
                    results.add(buildMethodResult("Исходная функция", origFunc.apply(targetX), points, origFunc));
                }
            }

            // 1. Лагранж
            results.add(buildMethodResult("Многочлен Лагранжа", service.lagrange(points, points.size(), targetX), points, x -> service.lagrange(points, points.size(), x)));

            // 2. Ньютон разд разности
            double[][] divDiff = service.dividedDifferences(points, points.size());
            double midX = (points.get(0).x + points.get(points.size() - 1).x) / 2.0;

           if (targetX <= midX) {
                results.add(buildMethodResult("Ньютон (раздел. разности, I)", 
                    service.newtonDivided(points, points.size(), divDiff, targetX), 
                    points, 
                    x -> service.newtonDivided(points, points.size(), divDiff, x)));
            } else {
                results.add(buildMethodResult("Ньютон (раздел. разности, II)", 
                    service.newtonDividedBackward(points, points.size(), divDiff, targetX), 
                    points, 
                    x -> service.newtonDividedBackward(points, points.size(), divDiff, x)));
            }

            if (isEquidistant) {
                double[][] finDiff = service.finiteDifferences(points, points.size());
                
                if (targetX <= midX) {
                    results.add(buildMethodResult("Ньютон (конечные разн., I)", 
                        service.newtonFiniteForward(points, points.size(), finDiff, targetX), 
                        points, 
                        x -> service.newtonFiniteForward(points, points.size(), finDiff, x)));
                } else {
                    results.add(buildMethodResult("Ньютон (конечные разн., II)", 
                        service.newtonFiniteBackward(points, points.size(), finDiff, targetX), 
                        points, 
                        x -> service.newtonFiniteBackward(points, points.size(), finDiff, x)));
                }

                double stirlingVal = service.stirling(points, points.size(), finDiff, targetX);
                if (!Double.isNaN(stirlingVal)) {
                    results.add(buildMethodResult("Стирлинг", stirlingVal, points, 
                        x -> service.stirling(points, points.size(), finDiff, x)));
                }

                double besselVal = service.bessel(points, points.size(), finDiff, targetX);
                if (!Double.isNaN(besselVal)) {
                    results.add(buildMethodResult("Бессель", besselVal, points, 
                        x -> service.bessel(points, points.size(), finDiff, x)));
                }
            }

            double minX = points.get(0).x;
            double maxX = points.get(points.size() - 1).x;
            String warning = (targetX < minX || targetX > maxX) ? "!!! Произошла экстраполяция функции, значения могут быть неточными" : null;

            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("diffTable", diffTable);
            response.put("isEquidistant", isEquidistant);
            response.put("warning", warning);

            ctx.json(response);
        });
        

        // 4. POST /api/generate-random
        app.post("/api/generate-random", ctx -> {
            GenerateRequest req = ctx.bodyAsClass(GenerateRequest.class);
            int count = req.count != null ? req.count : 10;

            if (count < 8 || count > 12) {
                throw new ValidationException("Количество точек должно быть от 8 до 12");
            }

            Random rand = new Random();
            double slope = 0.5 + rand.nextDouble() * 1.5;
            double intercept = 10.0 + rand.nextDouble() * 40.0;
            double currentX = 1.0;
            List<Point2D> points = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                currentX += 0.8 + rand.nextDouble() * 1.2;
                double noise = (rand.nextDouble() - 0.5) * 6.0;
                double y = slope * currentX + intercept + noise;
                points.add(new Point2D(
                        Math.round(currentX * 100.0) / 100.0,
                        Math.round(y * 100.0) / 100.0
                ));
            }

            ctx.json(points);
        });
    }

    public static class CalculateRequest {
        public double[][] points;
        public Double targetX;
        public Integer funcType;
    }

    public static class GenerateRequest {
        public Integer count;
    }

    private static Map<String, Object> buildMethodResult(String name, double targetValue, List<Point2D> points, Function<Double, Double> func) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("targetValue", targetValue);
        
        List<Point2D> plotData = new ArrayList<>();
        double minX = points.get(0).x;
        double maxX = points.get(points.size() - 1).x;
        

        double span = maxX - minX;
        if (span == 0) span = 1.0;

        double padding = span * 0.05;
        double plotStart = minX - padding;
        double plotEnd = maxX + padding;
        

        // double step = (plotEnd - plotStart) / 150.0; 
        double step = (plotEnd - plotStart) / ((points.size() + 1) * 100); 

        for (double x = plotStart; x <= plotEnd; x += step) {

            double y = func.apply(x);
            
            if (Double.isFinite(y)) {
                plotData.add(new Point2D(x, y));
            }
        }
        map.put("plotData", plotData);
        return map;
    }
}