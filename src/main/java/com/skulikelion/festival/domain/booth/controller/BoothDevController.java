package com.skulikelion.festival.domain.booth.controller;

import com.skulikelion.festival.domain.booth.dto.request.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothResponse;
import com.skulikelion.festival.domain.booth.service.BoothDevService;
import com.skulikelion.festival.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/booths")
@Tag(name = "BoothDev", description = "개발자용 Booth 관리 API")
public class BoothDevController {

  private final BoothDevService boothDevService;

  @Operation(
      summary = "[개발자] 새 부스 등록",
      description = "새로운 부스를 등록하고 생성된 부스 정보를 반환합니다. (201 Created)")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<BoothResponse>> createBooth(
      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @RequestPart(value = "booth")
      @Valid
      BoothRequest request,
      @Parameter(
          description = "부스 이미지들",
          content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
      @RequestPart(value = "images")
      List<MultipartFile> images) {
    BoothResponse response = boothDevService.createBooth(request, images);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success("부스 등록이 완료되었습니다.", response));
  }

  @Operation(summary = "[개발자] 부스 수정", description = "부스 이름과 비밀번호를 수정합니다. (200 OK)")
  @PutMapping
  public ResponseEntity<BaseResponse<BoothResponse>> updateBooth(
      @RequestBody @Valid BoothRequest request) {
    BoothResponse response = boothDevService.updateBooth(request);
    return ResponseEntity.ok(BaseResponse.success("부스 정보가 수정되었습니다.", response));
  }

  @Operation(summary = "[개발자] 부스 삭제", description = "생성된 부스를 삭제합니다. (200 OK)")
  @DeleteMapping("{id}")
  public ResponseEntity<BaseResponse<String>> deleteBooth(
      @Parameter(description = "삭제할 부스 식별자", example = "1") @PathVariable Long id) {
    boothDevService.deleteBooth(id);
    return ResponseEntity.ok(BaseResponse.success("부스가 삭제되었습니다."));
  }
}
