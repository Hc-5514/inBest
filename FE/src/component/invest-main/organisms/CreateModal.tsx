import Modal from "react-modal";
import { CONTENT_MODAL_STYLE, OVERLAY_MODAL_STYLE } from "../../../constant/MODAL_STYLE";
import InitGroup from "../molecules/InitGroup";
import SettingPeriod from "../molecules/SettingPeriod";
import { useCreateModal } from "./useCreateModal";
import ProgressCircle from "../molecules/ProgressCircle";
import group from "../../../asset/image/group.png";
import SettingSeedMoney from "../molecules/SettingSeedMoney";

interface Props {
  showModal: boolean;
  closeModal: () => void;
}

const CreateModal = ({ showModal, closeModal }: Props) => {
  const { onNextStep, groupSetting, dispatch, step, resetStepAndGroupSetting } = useCreateModal();

  const CreateModalStepComponent = [
    <InitGroup onNextStep={onNextStep} closeModal={closeModal} resetStepAndGroupSetting={resetStepAndGroupSetting} />,
    <SettingPeriod
      onNextStep={onNextStep}
      period={groupSetting.period}
      dispatch={dispatch}
      closeModal={closeModal}
      resetStepAndGroupSetting={resetStepAndGroupSetting}
    />,
    <SettingSeedMoney
      onNextStep={onNextStep}
      seedMoney={groupSetting.seedMoney}
      dispatch={dispatch}
      closeModal={closeModal}
      resetStepAndGroupSetting={resetStepAndGroupSetting}
    />,
  ];

  return (
    <Modal
      isOpen={showModal}
      ariaHideApp={false}
      onRequestClose={() => {
        closeModal();
        resetStepAndGroupSetting();
      }}
      closeTimeoutMS={300}
      style={{
        content: {
          ...CONTENT_MODAL_STYLE,
          width: "500px",
          height: step === 3 ? "700px" : "500px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          transition: "all 0.3s ease",
        },
        overlay: OVERLAY_MODAL_STYLE,
      }}
    >
      {step === 0 ? <img src={group} width={120} /> : <ProgressCircle step={step} />}
      {CreateModalStepComponent[step]}
    </Modal>
  );
};

export default CreateModal;
