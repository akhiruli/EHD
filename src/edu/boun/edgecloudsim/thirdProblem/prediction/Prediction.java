package edu.boun.edgecloudsim.thirdProblem.prediction;

public class Prediction {
    double direct;
    double diffuse;
    double temperature;
    double humidity;
    double wind_speed;
    double pressure;
    double precipitation;
    double zenith_angle;
    double azimuth_angle;
    double prediction;
    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getWind_speed() {
        return wind_speed;
    }

    public void setWind_speed(double wind_speed) {
        this.wind_speed = wind_speed;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }

    public double getZenith_angle() {
        return zenith_angle;
    }

    public void setZenith_angle(double zenith_angle) {
        this.zenith_angle = zenith_angle;
    }

    public double getAzimuth_angle() {
        return azimuth_angle;
    }

    public void setAzimuth_angle(double azimuth_angle) {
        this.azimuth_angle = azimuth_angle;
    }

    public double getPrediction() {
        return prediction;
    }

    public void setPrediction(double prediction) {
        this.prediction = prediction;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(double diffuse) {
        this.diffuse = diffuse;
    }

    public double getDirect() {
        return direct;
    }

    public void setDirect(double direct) {
        this.direct = direct;
    }
}
