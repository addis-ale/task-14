package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.campus.CampusRequest;
import com.exam.system.dto.campus.CampusResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.room.RoomRequest;
import com.exam.system.dto.room.RoomResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.CampusRoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CampusRoomController {

    private final CampusRoomService campusRoomService;
    private final ActiveRoleGuard activeRoleGuard;

    public CampusRoomController(CampusRoomService campusRoomService, ActiveRoleGuard activeRoleGuard) {
        this.campusRoomService = campusRoomService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping("/api/v1/campuses")
    public ResponseEntity<ApiResponse<PageData<CampusResponse>>> listCampuses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(campusRoomService.listCampuses(page, size)));
    }

    @PostMapping("/api/v1/campuses")
    public ResponseEntity<ApiResponse<CampusResponse>> createCampus(@Valid @RequestBody CampusRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(campusRoomService.createCampus(request)));
    }

    @PutMapping("/api/v1/campuses/{campusId}")
    public ResponseEntity<ApiResponse<CampusResponse>> updateCampus(@PathVariable Long campusId,
                                                                    @Valid @RequestBody CampusRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(campusRoomService.updateCampus(campusId, request)));
    }

    @DeleteMapping("/api/v1/campuses/{campusId}")
    public ResponseEntity<Void> deleteCampus(@PathVariable Long campusId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        campusRoomService.deleteCampus(campusId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/campuses/{campusId}/rooms")
    public ResponseEntity<ApiResponse<PageData<RoomResponse>>> listCampusRooms(@PathVariable Long campusId,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(campusRoomService.listCampusRooms(campusId, page, size)));
    }

    @PostMapping("/api/v1/campuses/{campusId}/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@PathVariable Long campusId,
                                                                @Valid @RequestBody RoomRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(campusRoomService.createRoom(campusId, request)));
    }

    @PutMapping("/api/v1/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(@PathVariable Long roomId,
                                                                @Valid @RequestBody RoomRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(campusRoomService.updateRoom(roomId, request)));
    }

    @DeleteMapping("/api/v1/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        campusRoomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
