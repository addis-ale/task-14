package com.exam.system.dto.dashboard;

public class DashboardStatsResponse {

    private long totalSessions;
    private long totalStudents;
    private long pendingReviews;
    private long pendingAntiCheatFlags;
    private long activeJobs;
    private long failedJobs;
    private long upcomingSessions;
    private java.util.List<java.util.Map<String, String>> recentActivity = new java.util.ArrayList<>();

    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }
    public long getPendingReviews() { return pendingReviews; }
    public void setPendingReviews(long pendingReviews) { this.pendingReviews = pendingReviews; }
    public long getPendingAntiCheatFlags() { return pendingAntiCheatFlags; }
    public void setPendingAntiCheatFlags(long pendingAntiCheatFlags) { this.pendingAntiCheatFlags = pendingAntiCheatFlags; }
    public long getActiveJobs() { return activeJobs; }
    public void setActiveJobs(long activeJobs) { this.activeJobs = activeJobs; }
    public long getFailedJobs() { return failedJobs; }
    public void setFailedJobs(long failedJobs) { this.failedJobs = failedJobs; }
    public long getUpcomingSessions() { return upcomingSessions; }
    public void setUpcomingSessions(long upcomingSessions) { this.upcomingSessions = upcomingSessions; }
    public java.util.List<java.util.Map<String, String>> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(java.util.List<java.util.Map<String, String>> recentActivity) { this.recentActivity = recentActivity; }
}
