package com.example.testspringboot.service;

import com.example.testspringboot.model.LocationStats;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@Getter
public class CoronaVirusDataService {
    final String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";
    private List<LocationStats> allStats = new ArrayList<>();
    DecimalFormat df = new DecimalFormat("###,###,###");
    
    @SneakyThrows
    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    public void fetchVirusData() {
        List<LocationStats> newStats = new ArrayList<>();
        
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VIRUS_DATA_URL)).build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        StringReader csvBodyReader = new StringReader(httpResponse.body());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);
        
        for (CSVRecord record : records) {
            LocationStats locationStats = new LocationStats();
            String state = record.get("Province/State");
            String country = record.get("Country/Region");
            int latestCases = Integer.parseInt(record.get(record.size() - 1));
            int prevDayCases = Integer.parseInt(record.get(record.size() - 2));
            
            locationStats.setState(state);
            locationStats.setCountry(country);
            locationStats.setLatestTotalCases(latestCases);
            locationStats.setDiffFromPrevCases(latestCases - prevDayCases);
            newStats.add(locationStats);
        }
        this.allStats = newStats;
    }
    
    @PostConstruct()
    public String getTotalCases() {
        return df.format(allStats.stream().mapToInt(LocationStats::getLatestTotalCases).sum());
    }
}
