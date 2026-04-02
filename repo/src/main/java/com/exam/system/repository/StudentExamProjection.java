package com.exam.system.repository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface StudentExamProjection {

    Long getSessionId();

    String getSubjectName();

    LocalDate getExamDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    String getRoomName();

    String getCampusName();

    Integer getSeatNumber();
}
