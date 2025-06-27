package com.teachhelper.dto;

import java.io.ByteArrayInputStream;

public record ExamExportData(ByteArrayInputStream inputStream, String examTitle) {} 