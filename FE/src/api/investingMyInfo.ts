import { instanceWithAuth } from "./interceptors";
import { MyAsset, MyInvestingRanking, MyStockList, RecentlyDeal } from "../type/InvestingMyInfo";
import userStore from "../store/userStore";

const apiWithAuth = instanceWithAuth("invest-service");

export const getMyAsset = async (simulationSeq: string | undefined): Promise<MyAsset> => {
  if (!simulationSeq) {
    throw new Error("simulationSeq is undefined");
  }

  const { userInfo } = userStore.getState();
  const { data } = await apiWithAuth.get(`group/${simulationSeq}/users/${userInfo?.seq}/asset-change`);
  return data;
};

export const getMyInvestingRanking = async (simulationSeq: string | undefined): Promise<MyInvestingRanking> => {
  if (!simulationSeq) {
    throw new Error("simulationSeq is undefined");
  }

  const { userInfo } = userStore.getState();
  const { data } = await apiWithAuth.get(`rank/simulation/${simulationSeq}/${userInfo?.seq}`);
  return data;
};

export const getMyStockList = async (simulationSeq: string | undefined): Promise<MyStockList> => {
  if (!simulationSeq) {
    throw new Error("simulationSeq is undefined");
  }

  const { userInfo } = userStore.getState();
  const { data } = await apiWithAuth.get(`group/${simulationSeq}/users/${userInfo?.seq}/stock`);
  return data;
};

export const getRecentlyDeal = async (simulationSeq: string | undefined): Promise<RecentlyDeal> => {
  if (!simulationSeq) {
    throw new Error("simulationSeq is undefined");
  }

  const { userInfo } = userStore.getState();
  const { data } = await apiWithAuth.get(`group/${simulationSeq}/users/${userInfo?.seq}/trading-history`, {
    params: {
      pageNo: 1,
      size: 7,
    },
  });
  return data;
};
