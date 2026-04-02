package com.exam.system.service;

import com.exam.system.dto.campus.CampusRequest;
import com.exam.system.dto.campus.CampusResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.room.RoomRequest;
import com.exam.system.dto.room.RoomResponse;

public interface CampusRoomService {

    PageData<CampusResponse> listCampuses(int page, int size);

    CampusResponse createCampus(CampusRequest request);

    CampusResponse updateCampus(Long campusId, CampusRequest request);

    void deleteCampus(Long campusId);

    PageData<RoomResponse> listCampusRooms(Long campusId, int page, int size);

    RoomResponse createRoom(Long campusId, RoomRequest request);

    RoomResponse updateRoom(Long roomId, RoomRequest request);

    void deleteRoom(Long roomId);
}
