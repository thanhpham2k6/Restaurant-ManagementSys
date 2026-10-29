package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeAttendance {
    private int id;
    private int userId;
    private String employeeName;
    private String roleName;
    private LocalDate workDate;
    private String shiftName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private BigDecimal hoursWorked;
    private String status; // "Đúng giờ", "Đi muộn", "Về sớm", "Vắng mặt"
    private String note;
    private LocalDateTime createdAt;

    public EmployeeAttendance() {}

    public EmployeeAttendance(int id, int userId, String employeeName, String roleName,
                              LocalDate workDate, String shiftName, LocalDateTime checkInTime,
                              LocalDateTime checkOutTime, BigDecimal hoursWorked,
                              String status, String note, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.employeeName = employeeName;
        this.roleName = roleName;
        this.workDate = workDate;
        this.shiftName = shiftName;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.hoursWorked = hoursWorked;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public BigDecimal getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
