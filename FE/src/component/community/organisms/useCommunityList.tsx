import { useState } from "react";
import { useNavigate } from "react-router-dom";

export const useCommunityList = () => {
  const [keyword, setKeyword] = useState("");
  const navigate = useNavigate();
  const onSearch = () => {
    navigate(`?${keyword}`);
  };
  return { keyword, setKeyword, onSearch };
};
