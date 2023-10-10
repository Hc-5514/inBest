import { useRankingPageNation } from "./useRankingPageNation";
import ReactPaginate from "react-paginate";

const GroupRankingPageNation = ({ pages }: { pages: number }) => {
  const { handlePageClick, page, firstOrLast } = useRankingPageNation();
  return (
    <div className="flex justify-center items-center">
      <div
        className="h-10 flex justify-center items-center  rounded-s-lg hover:cursor-pointer w-10 hover:bg-slate-300 "
        onClick={() => {
          firstOrLast(1);
        }}
      >
        {"<<"}
      </div>
      <ReactPaginate
        breakLabel={null}
        nextLabel=">"
        onPageChange={handlePageClick}
        pageRangeDisplayed={pages >= 5 ? 5 : pages}
        marginPagesDisplayed={0}
        pageCount={pages}
        previousLabel="<"
        renderOnZeroPageCount={null}
        pageClassName="border border-black w-full h-full flex justify-center items-center border-opacity-30"
        className="mb-10 flex justify-between items-center rounded-xl h-10 min-w-[16rem] bg-white mt-10 "
        pageLinkClassName="w-full h-full flex justify-center items-center"
        nextLinkClassName="w-full h-full flex justify-center items-center"
        activeClassName="bg-primary text-white border-0"
        forcePage={page ? Number(page) - 1 : 0}
        nextClassName="border border-black w-full h-full flex justify-center items-center rounded-e-lg  border-opacity-30 px-2"
        previousClassName="border border-black w-full h-full flex justify-center items-center rounded-s-lg border-opacity-30 px-2"
        previousLinkClassName="w-full h-full flex justify-center items-center"
      />
      <div
        className=" h-10 flex justify-center items-center  rounded-e-lg hover:cursor-pointer w-10 hover:bg-slate-300 "
        onClick={() => firstOrLast(pages)}
      >
        {">>"}
      </div>
    </div>
  );
};
export default GroupRankingPageNation;
