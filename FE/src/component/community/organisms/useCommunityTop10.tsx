import { useQuery } from "react-query";
import { getBoardTop10 } from "../../../api/board";

export const useCommunityTop10 = () => {
  const { data, error } = useQuery(["getBoardTop10"], () => getBoardTop10());
  const boardList = data?.board;
  if (error) console.log(error);
  console.log(boardList);
  return { boardList };
};
