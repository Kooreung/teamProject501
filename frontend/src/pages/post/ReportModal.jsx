import {
  Box,
  Button,
  Modal,
  ModalBody,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Radio,
  RadioGroup,
  Stack,
  Textarea,
  useToast,
} from "@chakra-ui/react";
import React, { useState } from "react";
import axios from "axios";

export function ReportModal({ isOpen, onClose, onOpen, postId }) {
  const [reportReason, setReportReason] = useState("1"); // 기본값 설정
  const [detailReason, setDetailReason] = useState("");
  const toast = useToast();

  function handleSubmitReport() {
    const reason =
      reportReason === "1"
        ? "부적절한 언어 사용"
        : reportReason === "2"
          ? "개인정보 노출"
          : reportReason === "3"
            ? "위치에 맞지 않는 게시물"
            : null;
    if (!reason) {
      alert("신고 이유를 선택해주세요.");
      return;
    }

    axios
      .post("/api/report/add", {
        postId: postId,
        reportReason: reason,
        reportDetailReason: detailReason,
      })
      .then((response) => {
        toast({
          status: "success",
          position: "bottom",
          isClosable: true,
          description: "신고완료",
        });
        onClose(); // 모달 닫기
      })
      .catch((error) => {
        toast({
          status: "error",
          isClosable: true,
          description: "신고실패",
          position: "bottom",
        });
      });
  }

  return (
    <Box>
      <Modal isOpen={isOpen} onClose={onClose}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>게시글 신고</ModalHeader>
          <ModalBody>게시글을 신고하시겠습니까?</ModalBody>
          <Box>
            <RadioGroup
              defaultValue={reportReason}
              onChange={setReportReason}
              ml={5}
            >
              <Stack spacing={5}>
                <Radio colorScheme="purple" value="1">
                  부적절한 언어 사용
                </Radio>
                <Radio colorScheme="purple" value="2">
                  개인정보 노출
                </Radio>
                <Radio colorScheme="purple" value="3">
                  위치에 맞지 않는 게시물
                </Radio>
              </Stack>
            </RadioGroup>
            <Box>
              <ModalBody>상세사유</ModalBody>
              <Textarea
                w={"89%"}
                ml={6}
                value={detailReason}
                onChange={(e) => setDetailReason(e.target.value)}
              />
            </Box>
          </Box>
          <ModalFooter>
            <Button onClick={handleSubmitReport}>확인</Button>
            <Button onClick={onClose}>취소</Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </Box>
  );
}
