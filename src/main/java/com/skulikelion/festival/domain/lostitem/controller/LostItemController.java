package com.skulikelion.festival.domain.lostitem.controller;

import com.skulikelion.festival.domain.lostitem.dto.request.LostItemRequest;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.entity.SortType;
import com.skulikelion.festival.domain.lostitem.service.LostItemService;
import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.minio.entity.PathName;
import com.skulikelion.festival.global.minio.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Lost Item Controller", description = "분실물 등록, 조회, 수정, 삭제 관련 API")
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

  private final LostItemService lostItemService;
  private final MinioService minioService;

  @Operation(
      summary = "[관리자] 분실물 등록",
      description = "분실물 이름, 이미지 URL, 습득 장소, 습득 날짜를 입력하여 분실물을 등록하는 API")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<LostItemResponse>> createLostItem(
      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
          @RequestPart(value = "dto")
          @Valid
      LostItemRequest dto,
      @Parameter(
              description = "업로드할 이미지 파일",
              content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
          @RequestPart(value = "file")
          MultipartFile file) {

    String imageUrl = minioService.uploadFile(file, PathName.LOSTITEM); // 이미지 업로드
    LostItemResponse response = lostItemService.createLostItem(dto, imageUrl); // imageUrl 함께 전달

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success("분실물 등록 성공", response));
  }

  @Operation(summary = "[관리자] 분실물 단일 조회", description = "ID로 특정 분실물 하나를 조회하는 API")
  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<LostItemResponse>> findLostItem(@PathVariable Long id) {
    LostItemResponse response = lostItemService.findOne(id);
    return ResponseEntity.ok(BaseResponse.success("분실물 조회 성공", response));
  }

  @Operation(
      summary = "[사용자] 분실물 전체 목록 조회",
      description =
          """
              `/api/lost-items` → 전체 분실물 목록 반환\n
              `/api/lost-items?name=이름` → '이름'을 포함하는 분실물만 반환\n
              `/api/lost-items?sort=LATEST` → 최신 순으로 정렬(대문자)\n
              `/api/lost-items?sort=OLDEST` → 오래된 순으로 정렬(대문자)\n
              `/api/lost-items?name=이름&sort=LATEST` → 이름 검색 + 최신순 정렬(대문자)\n
              `/api/lost-items?name=이름&sort=LATEST&page=0` → 이름 검색 + 최신순 정렬 + 첫 번째 페이지 조회
              """)
  @GetMapping
  public ResponseEntity<BaseResponse<Page<LostItemResponse>>> findAllLostItems(
      @RequestParam(required = false) String name,
      @RequestParam(required = false, defaultValue = "LATEST") SortType sort,
      @RequestParam(defaultValue = "0") int page) {
    Pageable pageable =
        PageRequest.of(
            page,
            6,
            sort == SortType.LATEST
                ? Sort.by("createdAt").descending()
                : Sort.by("createdAt").ascending());

    Page<LostItemResponse> data = lostItemService.findAll(name, sort, pageable);

    return ResponseEntity.ok(BaseResponse.success("분실물 조회 성공", data));
  }

  @Operation(
      summary = "[관리자] 분실물 정보 수정",
      description = "분실물의 이름, 이미지, 습득 장소, 습득 날짜 등의 정보를 수정하는 API")
  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<LostItemResponse>> updateLostItems(
      @PathVariable Long id,
      @RequestPart(value = "dto") @Valid LostItemRequest dto,
      @RequestPart(value = "file", required = false) MultipartFile file) {

    String imageUrl = null;
    if (file != null && !file.isEmpty()) {
      imageUrl = minioService.uploadFile(file, PathName.LOSTITEM);
    }

    LostItemResponse response = lostItemService.update(id, dto, imageUrl);
    return ResponseEntity.ok(BaseResponse.success("분실물 수정 성공", response));
  }

  @Operation(summary = "[관리자] 수령 여부 변경", description = "분실물이 수령되었는지 여부를 설정하는 API")
  @PutMapping("/{id}/returned")
  public ResponseEntity<BaseResponse<LostItemResponse>> setReturned(
      @PathVariable Long id, @RequestParam boolean returned) {
    LostItemResponse response = lostItemService.setReturned(id, returned);
    return ResponseEntity.ok(BaseResponse.success("수령 여부 변경 성공", response));
  }

  @Operation(summary = "[관리자] 분실물 삭제", description = "분실물 삭제 API (Soft Delete 방식)")
  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> deleteLostItems(@PathVariable Long id) {
    lostItemService.deleteById(id);
    return ResponseEntity.ok(BaseResponse.success("분실물 삭제 성공", null));
  }
}
