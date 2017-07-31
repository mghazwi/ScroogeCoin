import java.util.ArrayList;
import java.util.LinkedList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        Transaction.Input tempInput;
        Transaction.Output tempOutput;
        Transaction.Output txOutput;
        double inputSum = 0;
        double outputSum = 0;
        ArrayList<UTXO> utxos = new ArrayList<UTXO>();
        UTXO tempUtxo;
        for (int i=0; i<tx.numInputs(); i++) {
          tempInput = tx.getInput(i);

          tempUtxo = new UTXO(tempInput.prevTxHash, tempInput.outputIndex);
          //(3) check for double-spending
          for (int j=0; j<utxos.size(); j++) {
            if(utxos.get(j).equals(tempUtxo)){
              return false;
            }
          }
          utxos.add(tempUtxo);
          //(1)need to test how to chack if pool contains utxo.
          if(!this.utxoPool.contains(tempUtxo)){
            return false;
          }
          //(2) check signature
          tempOutput = utxoPool.getTxOutput(tempUtxo);

          if(!Crypto.verifySignature(tempOutput.address,tx.getRawDataToSign(i),tempInput.signature)){
            return false;
          }

          //(5) calculate input sum
          inputSum = inputSum + tempOutput.value;
        }
        //(4) output is not negative
        //(5) calculate output sum
        for (int i=0; i<tx.numOutputs(); i++) {
          txOutput = tx.getOutput(i);
          outputSum = outputSum + txOutput.value;
          if(txOutput.value < 0 ){
            return false;
          }
        }

        //(5) check input sum > output sum
        if(outputSum > inputSum){
          return false;
        }

        //if all checked and vaild return true.
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Transaction tempTx;
        Transaction[] validTxs;
        boolean change;
        int ref = -1;
        ArrayList<Transaction> txArray = new ArrayList<Transaction>();
        LinkedList<Transaction> txToProccess = new LinkedList<Transaction>();
        //put all txs to proccessing
        for (int i=0; i<possibleTxs.length; i++) {
          txToProccess.add(possibleTxs[i]);
        }

        //chack if vaild in the pool
        while (true) {
          change = false;
          if(txToProccess.size() < 1){
            break;
          }
          tempTx = txToProccess.removeFirst();
          if(isValidTx(tempTx)){
            txArray.add(tempTx);
            //update current pool
            validateTx(tempTx);
            change = true;
            continue;
          }
          //chack if referenced in given txs
          for (int j=0; j<tempTx.numInputs(); j++) {
            for (int k=0; k<txToProccess.size(); k++) {
              if(txToProccess.get(k).getHash().equals(tempTx.getInput(j).prevTxHash)){
                //what happen if referenced
                if (ref < k+1) {
                  ref = k+1;
                }
              }
            }
          }
          if(ref > 0){
            txToProccess.add(ref,tempTx);
          }
          ref = -1;
        }

        //put together the final approved list of txs
        validTxs = new Transaction[txArray.size()];
        for(int i=0;i<validTxs.length; i++){
          validTxs[i] = txArray.get(i);
        }
        //return valid txs
        return validTxs;
    }

// validate Transaction to the pool
    private void validateTx(Transaction tx){
      Transaction.Input tempInput;
      UTXO tempUtxo;
      Transaction.Output tempOutput;
      //remove all used UTXO
      for (int i=0; i<tx.numInputs(); i++) {
        tempInput = tx.getInput(i);
        tempUtxo = new UTXO(tempInput.prevTxHash, tempInput.outputIndex);
        if(this.utxoPool.contains(tempUtxo)){
          this.utxoPool.removeUTXO(tempUtxo);
        }else{
          //print info
          System.out.println("error: UTXO validated but not in the pool, utxoPool is incosistent");
        }
      }
      //add all unused UTXO
      for (int i=0; i<tx.numOutputs(); i++) {
        tempUtxo = new UTXO(tx.getHash(),i);
        tempOutput = tx.getOutput(i);
        this.utxoPool.addUTXO(tempUtxo, tempOutput);
      }
    }

}
