package com.rizzo.sarcasmotron.web;

import com.rizzo.sarcasmotron.domain.web.Trend;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
import com.rizzo.sarcasmotron.trend.TrendCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SarcasmotronController {

    @Autowired
    private TrendCalculator trendCalculator;

    @RequestMapping(value = "/trend", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Trend> seed(@RequestBody TrendRequest trendRequest){
        Trend trend = new Trend().setTrendLine(
                trendCalculator.calculateTrendLineForUser(
                        trendRequest.getUser(),
                        trendRequest.getPeriod(),
                        trendRequest.getInterval()));
        return new ResponseEntity<>(trend, HttpStatus.OK);
    }

}
