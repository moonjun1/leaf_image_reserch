package com.pam.pam.controller;


import com.pam.pam.dto.CropResponseDto;
import com.pam.pam.service.CropIdentifierService;
import lombok.Getter;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class CropController {

    private final CropIdentifierService cropIdentifierService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/identify")
    public String identifyCrop(@RequestParam("image") MultipartFile image, Model model) {
        try {
            if (image.isEmpty()) {
                model.addAttribute("error", "이미지를 선택해주세요.");
                return "index";
            }

            CropResponseDto response = cropIdentifierService.identifyCrop(image);
            model.addAttribute("result", response);

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "이미지 분석 중 오류가 발생했습니다: " + e.getMessage());
            return "index";
        }
    }
}
