package com.exam.system.service.impl;

import com.exam.system.config.InputSanitizer;
import com.exam.system.dto.campus.CampusRequest;
import com.exam.system.dto.campus.CampusResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.room.RoomRequest;
import com.exam.system.dto.room.RoomResponse;
import com.exam.system.entity.Campus;
import com.exam.system.entity.ExamRoom;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.CampusRepository;
import com.exam.system.repository.ExamRoomRepository;
import com.exam.system.repository.ProctorAssignRepository;
import com.exam.system.repository.RoomAssignmentRepository;
import com.exam.system.repository.SessionCandidateRepository;
import com.exam.system.service.CampusRoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampusRoomServiceImpl implements CampusRoomService {

    private final CampusRepository campusRepository;
    private final ExamRoomRepository roomRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final SessionCandidateRepository sessionCandidateRepository;
    private final ProctorAssignRepository proctorAssignRepository;
    private final PageDataBuilder pageDataBuilder;

    public CampusRoomServiceImpl(CampusRepository campusRepository,
                                 ExamRoomRepository roomRepository,
                                 RoomAssignmentRepository roomAssignmentRepository,
                                 SessionCandidateRepository sessionCandidateRepository,
                                 ProctorAssignRepository proctorAssignRepository,
                                 PageDataBuilder pageDataBuilder) {
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.sessionCandidateRepository = sessionCandidateRepository;
        this.proctorAssignRepository = proctorAssignRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<CampusResponse> listCampuses(int page, int size) {
        Page<Campus> campuses = campusRepository.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(100, size)));
        return pageDataBuilder.from(campuses, this::toCampusResponse, page, size);
    }

    @Override
    @Transactional
    public CampusResponse createCampus(CampusRequest request) {
        Campus campus = new Campus();
        campus.setName(InputSanitizer.sanitize(request.getName()));
        campus.setAddress(InputSanitizer.sanitize(request.getAddress()));
        return toCampusResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional
    public CampusResponse updateCampus(Long campusId, CampusRequest request) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Campus not found"));
        campus.setName(InputSanitizer.sanitize(request.getName()));
        campus.setAddress(InputSanitizer.sanitize(request.getAddress()));
        return toCampusResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional
    public void deleteCampus(Long campusId) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Campus not found"));
        if (roomRepository.countByCampusId(campusId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Cannot delete campus with existing rooms");
        }
        campusRepository.delete(campus);
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<RoomResponse> listCampusRooms(Long campusId, int page, int size) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Campus not found"));
        Page<ExamRoom> rooms = roomRepository.findByCampusId(campusId, PageRequest.of(Math.max(0, page - 1), Math.min(100, size)));
        return pageDataBuilder.from(rooms, room -> toRoomResponse(room, campus.getName(), 0), page, size);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(Long campusId, RoomRequest request) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Campus not found"));
        ExamRoom room = new ExamRoom();
        room.setCampusId(campusId);
        room.setName(InputSanitizer.sanitize(request.getName()));
        room.setCapacity(request.getCapacity());
        ExamRoom saved = roomRepository.save(room);
        return toRoomResponse(saved, campus.getName(), 0);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomRequest request) {
        ExamRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Room not found"));
        room.setName(InputSanitizer.sanitize(request.getName()));
        room.setCapacity(request.getCapacity());
        ExamRoom saved = roomRepository.save(room);
        String campusName = campusRepository.findById(saved.getCampusId()).map(Campus::getName).orElse(null);
        int assignedCount = (int) sessionCandidateRepository.countByRoomId(roomId);
        return toRoomResponse(saved, campusName, assignedCount);
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {
        ExamRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Room not found"));
        if (roomAssignmentRepository.countByRoomId(roomId) > 0
                || sessionCandidateRepository.countByRoomId(roomId) > 0
                || proctorAssignRepository.countByRoomId(roomId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Cannot delete room referenced by assignments");
        }
        roomRepository.delete(room);
    }

    private CampusResponse toCampusResponse(Campus campus) {
        CampusResponse response = new CampusResponse();
        response.setId(campus.getId());
        response.setName(campus.getName());
        response.setAddress(campus.getAddress());
        return response;
    }

    private RoomResponse toRoomResponse(ExamRoom room, String campusName, int assignedCount) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setCampusId(room.getCampusId());
        response.setCampusName(campusName);
        response.setName(room.getName());
        response.setCapacity(room.getCapacity());
        response.setAssignedCount(assignedCount);
        response.setRemainingCapacity(Math.max(0, room.getCapacity() - assignedCount));
        return response;
    }
}
