import MyGroup from "../molecules/MyGroup";
import group from "../../../asset/image/group.png";
import { AiOutlinePlusCircle } from "react-icons/ai";
import modalStore from "../../../store/modalStore";

const MyGroupList = () => {
  const { openModal } = modalStore();
  const data = [
    {
      title: "title1",
      memberCnt: 1,
      seedMoney: 10000,
      avgTier: 100,
      progressState: "inProgress",
      groupCode: "groupCode1",
    },
    {
      title: "title2",
      memberCnt: 2,
      seedMoney: 0,
      avgTier: 200,
      progressState: "beforeStart",
      groupCode: "groupCode2",
    },
    {
      title: "title3",
      memberCnt: 3,
      seedMoney: 30000,
      avgTier: 300,
      progressState: "beforeStart",
      groupCode: "groupCode3",
    },
  ];
  return (
    <div className="w-4/5 flex flex-col text-center px-4 shadow-component">
      <div className=" flex items-center gap-4">
        <img src={group} width={70} />
        <h3 className=" text-left">내 그룹</h3>
      </div>
      <div className="flex justify-between w-full border-t-2 border-b-2 px-4">
        <p className="w-2">#</p>
        <p className="w-24">그룹 이름</p>
        <p className="w-16">현재 인원</p>
        <p className="w-32">시드 머니</p>
        <p className="w-16">평균 티어</p>
        <p className="w-16">진행 상태</p>
      </div>

      {data.map((group, index) => (
        <MyGroup
          key={group.groupCode}
          index={index}
          title={group.title}
          memberCnt={group.memberCnt}
          seedMoney={group.seedMoney}
          avgTier={group.avgTier}
          progressState={group.progressState}
          groupCode={group.groupCode}
        />
      ))}
      <div
        onClick={() => openModal("createGroup")}
        className="text-gray-500  rounded-lg hover:text-black flex cursor-pointer items-center justify-center gap-2 py-2 hover:bg-gray-200 hover:bg-opacity-50 transition-colors duration-300"
      >
        <AiOutlinePlusCircle
          style={{
            color: "#6CE061",
            width: "1.5rem",
            height: "1.5rem",
          }}
        />
        그룹 생성
      </div>
    </div>
  );
};
export default MyGroupList;
