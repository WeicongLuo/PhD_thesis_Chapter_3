package com.demo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.demo.service.IDemoService;
import com.demo.util.DownloadUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

    public void importCsv(MultipartFile startFile, MultipartFile endFile, String mapKey,
                          HttpServletRequest request, HttpServletResponse response) {
        try {
            int sleepTime = 1500;
            int indexNum = 50; 
           
            Map<String, Object> endData = getEndData(endFile, indexNum);
            List<Map<String, String>> endObjectData = (List<Map<String, String>>) endData.get("endObjectData");
            List<String> coordinateList = (List<String>) endData.get("coordinateList");
            String sheetName = "两点间驾车时间批量计算结果"
            HSSFWorkbook workbook = new HSSFWorkbook()
            HSSFSheet sheet = workbook.createSheet(sheetName)
            sheet.setDefaultColumnWidth(12)
           
            HSSFRow row = sheet.createRow(0);
            row.createCell(0).setCellValue("objectid");
            row.createCell(1).setCellValue("OBJECTID");
            row.createCell(2).setCellValue("name");
            row.createCell(3).setCellValue("longitude");
            row.createCell(4).setCellValue("latitude");
            row.createCell(5).setCellValue("address");
            row.createCell(6).setCellValue("OBJECTID");
            row.createCell(7).setCellValue("name");
            row.createCell(8).setCellValue("longtitude");
            row.createCell(9).setCellValue("latitude");
            row.createCell(10).setCellValue("address");
            row.createCell(11).setCellValue("travel time(s)");
            row.createCell(12).setCellValue("distace(m)");

            
            InputStream startIS = startFile.getInputStream();
            InputStreamReader startISR = new InputStreamReader(startIS, "GBK");
            BufferedReader readerStart = new BufferedReader(startISR);
            readerStart.readLine()
            String lineStart;

            int tableIndex = 0;
            while ((lineStart = readerStart.readLine()) != null) {
                String[] item = lineStart.split(",")

                String objectid = item[0].trim(); 
                String name = item[1].trim(); 
                String longitude = item[2].trim(); 
                String latitude = item[3].trim(); 
                String address = item[4].trim(); 

           
                int totalIndex = 0
                for (String endCoordinates : coordinateList) {
                    Thread.sleep(sleepTime)
                    JSONArray durationDistances = getBatchDurationDistance(endCoordinates, latitude + "," + longitude, mapKey);
                    if (durationDistances == null) {
                        continue;
                    }

                    for (Object durationDistance : durationDistances) {
                        Map durationDistanceMap = (Map) durationDistance;
                        Map duration = (Map) durationDistanceMap.get("duration");
                        Map distance = (Map) durationDistanceMap.get("distance");
                        // {"text":"16分钟","value":964}
                        // {"text":"4.5公里","value":4543}
                        Map<String, String> endObject = endObjectData.get(totalIndex)
                        totalIndex++;

                        tableIndex++;
                        row = sheet.createRow(tableIndex)
                        row.createCell(0).setCellValue(tableIndex);
                        row.createCell(1).setCellValue(endObject.get("objectid"));
                        row.createCell(2).setCellValue(endObject.get("name"));
                        row.createCell(3).setCellValue(endObject.get("longitude"));
                        row.createCell(4).setCellValue(endObject.get("latitude"));
                        row.createCell(5).setCellValue(endObject.get("address"));
                        row.createCell(6).setCellValue(objectid);
                        row.createCell(7).setCellValue(name);
                        row.createCell(8).setCellValue(longitude);
                        row.createCell(9).setCellValue(latitude);
                        row.createCell(10).setCellValue(address);
                        row.createCell(11).setCellValue(duration.get("value").toString());
                        row.createCell(12).setCellValue(distance.get("value").toString());
                    }
                }
            }
            DownloadUtil.excelFile(request, response, sheetName, workbook);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private Map<String, Object> getEndData(MultipartFile endFile, int index) throws Exception {
        Map<String, Object> result = new HashMap<>(); 
        List<Map<String, String>> endData = new ArrayList<>(); 
        List<String> coordinateList = new ArrayList<>(); 
        StringBuilder coordinates = new StringBuilder(); 
        InputStream endIS = endFile.getInputStream();
        InputStreamReader endISR = new InputStreamReader(endIS, "GBK");
        BufferedReader readerEnd = new BufferedReader(endISR);
        readerEnd.readLine();
        String lineEnd;
        int indexNum = 0; 
        while ((lineEnd = readerEnd.readLine()) != null) {
            indexNum++;
            String[] item = lineEnd.split(","); 
            Map<String, String> oneRowData = new HashMap<>(); 
            String objectid = item[0].trim();
            String name = item[1].trim(); 
            String longitude = item[2].trim(); 
            String latitude = item[3].trim(); 
            String address = item[4].trim(); 

            oneRowData.put("objectid", objectid);
            oneRowData.put("name", name);
            oneRowData.put("longitude", longitude);
            oneRowData.put("latitude", latitude);
            oneRowData.put("address", address);

            coordinates.append(latitude).append(",").append(longitude).append("|");
            if (indexNum >= index) {
                coordinateList.add(coordinates.toString().substring(0, coordinates.length() - 1));
                indexNum = 0;
                coordinates = new StringBuilder();
            }
            endData.add(oneRowData);
        }
       
        if (coordinates.length() != 0) {
            coordinateList.add(coordinates.toString().substring(0, coordinates.length() - 1));
        }
        result.put("endObjectData", endData);
        result.put("coordinateList", coordinateList);
        return result;
    }


    private JSONArray getBatchDurationDistance(String start, String end, String mapKey) {
        RestTemplate restTemplate = new RestTemplate();
      
        String url = "http://api.map.baidu.com/routematrix/v2/driving?output=json&tactics=12" +
                "&origins=" + start + "&destinations=" + end + "&ak=" + mapKey;
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        JSONObject body = JSON.parseObject(responseEntity.getBody());
        // 0：成功;1：服务器内部错误;2：参数错误
        String status = body.get("status").toString();
        if ("0".equals(status)) {
            System.out.println(status+":"+body.get("message").toString());
            return (JSONArray) body.get("result");
        }
        System.err.println(status+":"+body.get("message").toString());
        return null;
    }

}
