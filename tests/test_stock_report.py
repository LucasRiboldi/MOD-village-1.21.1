import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "stock_report.py"
SPEC = importlib.util.spec_from_file_location("stock_report", SCRIPT)
stock_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(stock_report)


class BlockAdviceTest(unittest.TestCase):
    """Regra 342: o registro de blocos sugere subir, descer ou manter a reserva."""

    LOG = [
        "VC_BLOCKS version=1 colony=ab12cd34 day=3 blocks=3 oak_planks=V,12,4,30,6,0 "
        "crafting_table=S,1,5,0,0,0 cobblestone=R,256,90,10,0,36",
        "VC_BLOCKS version=1 colony=ab12cd34 day=4 blocks=2 oak_planks=V,12,20,10,0,0 crafting_table=S,1,5,0,0,0",
    ]

    def test_a_block_the_build_waited_for_below_the_reserve_goes_up(self):
        blocks = stock_report.collect_blocks(self.LOG)
        self.assertEqual(40, blocks["oak_planks"]["used"])
        self.assertEqual(1, blocks["oak_planks"]["short_days"])
        self.assertEqual("SUBIR", stock_report.block_advice(blocks["oak_planks"]))

    def test_a_block_never_used_and_always_above_the_reserve_may_go_down(self):
        blocks = stock_report.collect_blocks(self.LOG)
        self.assertEqual("DESCER", stock_report.block_advice(blocks["crafting_table"]))

    def test_a_raw_block_that_surged_is_a_gathering_shortage(self):
        blocks = stock_report.collect_blocks(self.LOG)
        self.assertEqual("FALTA CRU (coleta)", stock_report.block_advice(blocks["cobblestone"]))


class KeepAdviceTest(unittest.TestCase):
    """Regra 343: a trava que nunca barra pode sair; a que barra mais da metade segura demais."""

    LOG = [
        "VC_KEEP version=1 day=3 locks=3 wool=16,40,0,30 upkeep=36,10,9,12 wheat=8,20,5,4",
        "VC_KEEP version=1 day=4 locks=1 wool=16,10,0,25",
    ]

    def test_each_lock_gets_its_advice(self):
        keeps = stock_report.collect_keeps(self.LOG)
        self.assertEqual(50, keeps["wool"]["checks"])
        self.assertEqual(25, keeps["wool"]["lowest"])
        self.assertEqual("NÃO AGE (pode sair)", stock_report.keep_advice(keeps["wool"]))
        self.assertEqual("SEGURA DEMAIS (descer)", stock_report.keep_advice(keeps["upkeep"]))
        self.assertEqual("ok", stock_report.keep_advice(keeps["wheat"]))
