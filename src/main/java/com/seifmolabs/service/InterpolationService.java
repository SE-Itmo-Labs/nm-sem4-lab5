package com.seifmolabs.service;

import java.util.List;

import com.seifmolabs.objects.Point2D;

public class InterpolationService {

    // Проверка узлов на равноотстоящесть
    public boolean isEquidistant(List<Point2D> points, int n) {
        if (n < 2) return false;
        double h = points.get(1).x - points.get(0).x;
        for (int i = 1; i < n; i++) {
            if (Math.abs((points.get(i).x - points.get(i - 1).x) - h) > 1e-4) {
                return false;
            }
        }
        return true;
    }

    // Многочлен Лагранжа
    public double lagrange(List<Point2D> points, int n, double x) {
        double result = 0.0;
        for (int i = 0; i < n; i++) {
            double term = points.get(i).y;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    term *= (x - points.get(j).x) / (points.get(i).x - points.get(j).x);
                }
            }
            result += term;
        }
        return result;
    }

    // Таблица конечных разностей (для равноотстоящих узлов)
    public double[][] finiteDifferences(List<Point2D> points, int n) {
        double[][] diff = new double[n][n];
        for (int i = 0; i < n; i++) diff[i][0] = points.get(i).y;

        for (int j = 1; j < n; j++) {
            for (int i = 0; i < n - j; i++) {
                diff[i][j] = diff[i + 1][j - 1] - diff[i][j - 1];
            }
        }
        return diff;
    }

    // Таблица разделенных разностей (для неравноотстоящих)
    public double[][] dividedDifferences(List<Point2D> points, int n) {
        double[][] diff = new double[n][n];
        for (int i = 0; i < n; i++) diff[i][0] = points.get(i).y;

        for (int j = 1; j < n; j++) {
            for (int i = 0; i < n - j; i++) {
                diff[i][j] = (diff[i + 1][j - 1] - diff[i][j - 1]) / (points.get(i + j).x - points.get(i).x);
            }
        }
        return diff;
    }

    // Ньютон I с разделенными разностями
    public double newtonDivided(List<Point2D> points, int n, double[][] diff, double targetX) {
        double result = diff[0][0];
        double product = 1.0;
        for (int i = 1; i < n; i++) {
            product *= (targetX - points.get(i - 1).x);
            result += diff[0][i] * product;
        }
        return result;
    }
    // Ньютон II с разделенными разностями
    public double newtonDividedBackward(List<Point2D> points, int n, double[][] diff, double targetX) {
        double result = diff[n - 1][0];
        double product = 1.0;
        for (int i = 1; i < n; i++) {
            product *= (targetX - points.get(n - i).x);
            result += diff[n - 1 - i][i] * product;
        }
        return result;
    }

    // Ньютон I с конечными разностями
    public double newtonFiniteForward(List<Point2D> points, int n, double[][] diff, double targetX) {
        double h = points.get(1).x - points.get(0).x;
        double t = (targetX - points.get(0).x) / h;
        
        double result = diff[0][0];
        double tTerm = 1.0;
        double fact = 1.0;
        
        for (int i = 1; i < n; i++) {
            tTerm *= (t - i + 1);
            fact *= i;
            result += (tTerm * diff[0][i]) / fact;
        }
        return result;
    }

    // Ньютон II с конечными разностями
    public double newtonFiniteBackward(List<Point2D> points, int n, double[][] diff, double targetX) {
        double h = points.get(1).x - points.get(0).x;
        double t = (targetX - points.get(n - 1).x) / h;
        
        double result = diff[n - 1][0];
        double tTerm = 1.0;
        double fact = 1.0;
        
        for (int i = 1; i < n; i++) {
            tTerm *= (t + i - 1);
            fact *= i;
            result += (tTerm * diff[n - 1 - i][i]) / fact;
        }
        return result;
    }

    private double factorial(int n) {
        double result = 1.0;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    private double stirlingTerm(double t, int order) {
        if (order == 1) return t;
        if (order == 2) return t * t;

        double term = (order % 2 == 0) ? t * t : t;
        int limit = (order % 2 == 0) ? order / 2 - 1 : (order - 1) / 2;
        for (int i = 1; i <= limit; i++) {
            term *= (t * t - (i * i));
        }
        return term;
    }

    private double besselTerm(double t, int order) {
        if (order == 1) return t - 0.5;
        if (order == 2) return t * (t - 1.0);

        double term = (order % 2 == 0) ? t * (t - 1.0) : (t - 0.5) * t * (t - 1.0);
        int m = order / 2;
        for (int i = 2; i <= m; i++) {
            term *= (t - i) * (t + i - 1.0);
        }
        return term;
    }

    public double stirling(List<Point2D> points, int n, double[][] diff, double x) {

        if (n < 3 || n % 2 == 0 || !isEquidistant(points, n)) return Double.NaN;

        int centerIndex = n / 2;
        double h = points.get(1).x - points.get(0).x;
        double t = (x - points.get(centerIndex).x) / h;

        double result = diff[centerIndex][0];

        if (n > 1) {
            double d1Left = (centerIndex - 1 >= 0 && centerIndex - 1 < n - 1) ? diff[centerIndex - 1][1] : 0.0;
            double d1Right = (centerIndex >= 0 && centerIndex < n - 1) ? diff[centerIndex][1] : 0.0;
            result += t * (d1Left + d1Right) / 2.0;
        }

        for (int k = 2; k < n; k++) {
            double term = stirlingTerm(t, k);
            double deltaY = 0.0;

            if (k % 2 == 0) {
                int m = k / 2;
                int idx = centerIndex - m;
                if (idx >= 0 && idx < n - k) {
                    deltaY = diff[idx][k];
                } else {
                    break;
                }
            } else {
                int m = (k - 1) / 2;
                int idx1 = centerIndex - m - 1;
                int idx2 = centerIndex - m;
                if (idx1 >= 0 && idx1 < n - k && idx2 >= 0 && idx2 < n - k) {
                    deltaY = (diff[idx1][k] + diff[idx2][k]) / 2.0;
                } else {
                    break;
                }
            }
            result += (term * deltaY) / factorial(k);
        }
        return result;
    }

    public double bessel(List<Point2D> points, int n, double[][] diff, double x) {

        if (n < 4 || n % 2 == 1 || !isEquidistant(points, n)) return Double.NaN;

        int start_index = n / 2 - 1;

        double h = points.get(1).x - points.get(0).x;
        double t = (x - points.get(start_index).x) / h;

        double y0 = diff[start_index][0];
        double y1 = diff[start_index + 1][0];

        double result = (y0 + y1) / 2.0;

        for (int k = 1; k < n; k++) {

            double deltaY;

            double term = besselTerm(t, k);

            if (k == 1) {
                if (start_index >= 0 && start_index < n - 1) {

                    deltaY = diff[start_index][1];
                } else {
                    break;
                }
            } else if (k % 2 == 0) {

                int m = k / 2;

                int idxA = start_index - m;
                int idxB = start_index - m + 1;

                if (idxA >= 0 && 
                    idxA < n - k && 
                    idxB >= 0 && 
                    idxB < n - k
                ) {
                    deltaY = (diff[idxA][k] + diff[idxB][k]) / 2.0;

                } else {

                    break;
                }
            } else {
                
                int m = (k - 1) / 2;
                int idx = start_index - m;

                if (idx >= 0 && idx < n - k) {

                    deltaY = diff[idx][k];
                } else {

                    break;
                }
            }
            result += (term * deltaY) / factorial(k);
        }
        return result;
    }
}