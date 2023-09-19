import Period from "../organisms/Period";
import Ranking from "../organisms/Ranking";
import SeedMoney from "../organisms/SeedMoney";
import BestPick from "../organisms/BestPick";

interface Props {
  groupCode: string | undefined;
}

const InvestingInfo = ({ groupCode }: Props) => {
  console.log(groupCode);
  return (
    <div className=" grid grid-cols-9 grid-rows-4 gap-10">
      <Ranking />
      <Period />
      <SeedMoney />
      <BestPick />
    </div>
  );
};
export default InvestingInfo;
