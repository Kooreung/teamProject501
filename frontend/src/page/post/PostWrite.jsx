import React, { useState } from "react";
import {
  Box,
  Button,
  Flex,
  FormControl,
  FormLabel,
  Input,
  Modal,
  ModalBody,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Textarea,
  Tooltip,
  useDisclosure,
  useToast,
} from "@chakra-ui/react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import SmartEditor from "../../component/SmartEditor.jsx";
import MapAdd from "../../MapAdd.jsx";

function PostWrite() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [selectedPlaces, setSelectedPlaces] = useState([]);

  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();
  const {
    isOpen: isModalOpenOfSave,
    onOpen: onModalOpenOfSave,
    onClose: onModalCloseOfSave,
  } = useDisclosure();
  const {
    isOpen: isModalOpenOfCancel,
    onOpen: onModalOpenOfCancel,
    onClose: onModalCloseOfCancel,
  } = useDisclosure();

  // 저장 버튼 비활성화 조건
  let disableSaveButton = "able";
  if (title.trim().length === 0) {
    disableSaveButton = "disableToTitle";
  }
  if (content.trim().length === 0) {
    disableSaveButton = "disableToContent";
  }

  // 저장 버튼 클릭 시
  function handleClickSave() {
    setLoading(true);
    axios
      .postForm("/api/post/add", { title, content })
      .then((res) => {
        const postId = res.data;

        axios
          .post(
            "/api/place/add",
            selectedPlaces.map((place) => ({
              placeName: place.place_name,
              placeUrl: place.place_url,
              address: place.address_name,
              category: place.category,
              latitude: parseFloat(place.y),
              longitude: parseFloat(place.x),
              postId: postId,
            })),
          )
          .then(() => {
            console.log("장소가 성공적으로 서버에 전송되었습니다.");
            navigate(`/post/${res.data}`);
            toast({
              status: "success",
              position: "bottom",
              description: "게시글이 등록되었습니다.",
            });
          })
          .catch((error) => {
            console.error(
              "장소를 서버에 전송하는 중 오류가 발생했습니다:",
              error,
            );
          });
      })
      .catch()
      .finally(() => setLoading(false));
  }

  // 취소 버튼 클릭 시
  function handleClickCancel() {
    navigate("/post/list");
    toast({
      status: "info",
      position: "bottom",
      description: "게시글 작성이 취소되었습니다.",
    });
  }

  return (
    <Flex direction={"column"} align={"center"}>
      <Flex direction={"column"} align={"center"}>
        <Box w={"540px"} bg={"lightgray"} my={"2rem"}>
          <Box align={"left"} mb={"1rem"}>
            <FormControl>
              <FormLabel>제목</FormLabel>
              <Input
                placeholder={"제목을 작성해주세요."}
                onChange={(e) => setTitle(e.target.value)}
              ></Input>
            </FormControl>
          </Box>
          {/*<option value={"서울01"}>강남/역삼</option>*/}
          {/*<option value={"서울02"}>서초/교대/방배</option>*/}
          {/*<option value={"서울03"}>잠실/송파/강동</option>*/}
          {/*<option value={"서울04"}>건대/성수/왕십리</option>*/}
          {/*<option value={"서울05"}>성북/노원/중랑</option>*/}
          {/*<option value={"서울06"}>종로/중구</option>*/}
          {/*<option value={"서울07"}>용산/이태원/한남</option>*/}
          {/*<option value={"서울08"}>홍대/합정/마포</option>*/}
          {/*<option value={"서울09"}>영등포/여의도/강서</option>*/}
          {/*<option value={"서울10"}>구로/관악/동작</option>*/}
        </Box>
        {/* Todo 장소 내용 표기 필요 */}
        {/*<Box*/}
        {/*  w={{ base: "720px", lg: "1080px" }}*/}
        {/*  h={"160px"}*/}
        {/*  bg={"lightgray"}*/}
        {/*  my={"32px"}*/}
        {/*>*/}
        {/*  장소 선택*/}
        {/*</Box>*/}
        <Box w={"576px"} bg={"lightgray"} my={"32px"}>
          <MapAdd
            selectedPlaces={selectedPlaces}
            setSelectedPlaces={setSelectedPlaces}
          />
        </Box>
        <Box>
          <Box>
            <Box w={"720px"} bg={"lightgray"} my={"32px"}>
              <Box align={"left"} my={10}>
                <FormControl>
                  <FormLabel>설명</FormLabel>
                  <Textarea
                    h={200}
                    placeholder={"내용을 작성해주세요."}
                    onChange={(e) => setContent(e.target.value)}
                  ></Textarea>
                  <SmartEditor />
                </FormControl>
              </Box>
            </Box>
            <Box align={"left"} my={10}>
              <Tooltip
                hasArrow
                isDisabled={disableSaveButton === "able"}
                label={
                  disableSaveButton === "disableToTitle"
                    ? "제목을 확인해주세요."
                    : disableSaveButton === "disableToContent"
                      ? "내용을 확인해주세요."
                      : ""
                }
              >
                <Button
                  onClick={onModalOpenOfSave}
                  isLoading={loading}
                  isDisabled={disableSaveButton !== "able"}
                >
                  등록
                </Button>
              </Tooltip>
              <Button onClick={onModalOpenOfCancel}>취소</Button>
              {/* Todo 게시글 작성 중 임시저장 필요 */}
              {/* TODO 게시글 수정하다가 나가려고 하면 Modal 표기 */}
            </Box>
          </Box>
        </Box>
      </Flex>

      {/* 게시글 작성 저장 Modal */}
      <Modal isOpen={isModalOpenOfSave} onClose={onModalCloseOfSave}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>게시글 저장</ModalHeader>
          <ModalBody>게시글을 등록하시겠습니까?</ModalBody>
          <ModalFooter>
            <Flex>
              <Button onClick={handleClickSave}>확인</Button>
              <Button onClick={onModalCloseOfSave}>취소</Button>
            </Flex>
          </ModalFooter>
        </ModalContent>
      </Modal>

      {/* 게시글 작성 취소 Modal */}
      <Modal isOpen={isModalOpenOfCancel} onClose={onModalCloseOfCancel}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>게시글 작성 취소</ModalHeader>
          <ModalBody>작성을 취소하시겠습니까?</ModalBody>
          <ModalFooter>
            <Flex>
              <Button onClick={handleClickCancel}>확인</Button>
              <Button onClick={onModalCloseOfCancel}>취소</Button>
            </Flex>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </Flex>
  );
}

export default PostWrite;
