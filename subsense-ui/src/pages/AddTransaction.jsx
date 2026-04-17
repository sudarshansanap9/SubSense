
import { useState, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, CheckCircle, UploadCloud, FileText, X, LayoutList, PenTool } from 'lucide-react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { useDropzone } from 'react-dropzone';
import Papa from 'papaparse';

const AddTransaction = () => {
  const { user } = useAuthStore();
  const [activeTab, setActiveTab] = useState('manual'); // 'manual' or 'csv'
  
  // Manual State
  const [formData, setFormData] = useState({
    userId: user?.userId || '',
    amount: '',
    merchant: '',
    category: '',
    timestamp: new Date().toISOString().slice(0, 16)
  });
  
  // App State
  const [isSuccess, setIsSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  
  // CSV State
  const [csvFile, setCsvFile] = useState(null);
  const [batchProgress, setBatchProgress] = useState(0);
  const [batchTotal, setBatchTotal] = useState(0);

  const handleManualSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    
    try {
      const payload = { ...formData, timestamp: new Date(formData.timestamp).toISOString() };
      await axios.post('http://localhost:8084/transactions', payload, {
         headers: { Authorization: `Bearer ${useAuthStore.getState().accessToken}` }
      });
      setIsSuccess(true);
      setTimeout(() => setIsSuccess(false), 3000);
      setFormData({ ...formData, amount: '', merchant: '', category: '' });
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to dispatch transaction. Ensure Port 8084 is running.');
    } finally {
      setIsLoading(false);
    }
  };

  const onDrop = useCallback(acceptedFiles => {
    if (acceptedFiles.length > 0) {
      setCsvFile(acceptedFiles[0]);
      setError('');
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ 
    onDrop,
    accept: { 'text/csv': ['.csv'] },
    maxFiles: 1
  });

  const getFuzzyKey = (row, possibleKeys) => {
    const keys = Object.keys(row);
    for (let p of possibleKeys) {
      const matched = keys.find(k => k.toLowerCase().includes(p));
      if (matched) return row[matched];
    }
    return null;
  };

  const handleCsvSubmit = async () => {
    if (!csvFile) return;
    setIsLoading(true);
    setError('');
    
    Papa.parse(csvFile, {
      header: true,
      skipEmptyLines: true,
      complete: async (results) => {
        try {
          const rows = results.data;
          setBatchTotal(rows.length);
          setBatchProgress(0);
          let successCount = 0;

          for (let row of rows) {
            const rawAmount = getFuzzyKey(row, ['amount', 'cost', 'price', 'value', 'charge']) || '0';
            const rawMerchant = getFuzzyKey(row, ['merchant', 'vendor', 'name', 'payee', 'desc']) || 'Unknown';
            const rawCategory = getFuzzyKey(row, ['category', 'type', 'group']) || 'General';
            const rawDate = getFuzzyKey(row, ['date', 'time', 'stamp']) || new Date().toISOString();

            let isoDate = new Date().toISOString();
            try { isoDate = new Date(rawDate).toISOString(); } catch(e) {}
            
            let parsedAmount = parseFloat(rawAmount.toString().replace(/[^0-9.-]+/g,""));
            if (isNaN(parsedAmount)) parsedAmount = 0;
            // Bank statements usually write debits as negatives, enforce absolute value for backend!
            parsedAmount = Math.abs(parsedAmount);

            const payload = {
              userId: user?.userId,
              amount: parsedAmount,
              merchant: rawMerchant,
              category: rawCategory,
              timestamp: isoDate
            };

            try {
              await axios.post('http://localhost:8084/transactions', payload, {
                headers: { Authorization: `Bearer ${useAuthStore.getState().accessToken}` }
              });
            } catch (postErr) {
              if (postErr.response?.status === 409) {
                console.warn(`Skipping duplicate row: ${rawMerchant}`); // Gracefully bypass duplicates
              } else {
                throw postErr;
              }
            }

            successCount++;
            setBatchProgress(successCount);
          }

          setIsSuccess(true);
          setTimeout(() => { setIsSuccess(false); setCsvFile(null); setBatchProgress(0); setBatchTotal(0); }, 3000);
        } catch (err) {
          console.error(err);
          setError('Batch processing failed midway. Check console for details.');
        } finally {
          setIsLoading(false);
        }
      },
      error: (err) => {
        setError(`Failed to parse CSV: ${err.message}`);
        setIsLoading(false);
      }
    });
  };

  return (
    <div className="max-w-3xl mx-auto py-8 animate-in fade-in duration-500">
      <Link to="/" className="inline-flex items-center text-slate-400 hover:text-primary mb-6 transition">
        <ArrowLeft size={16} className="mr-2" />
        Back to Dashboard
      </Link>

      <div className="glass-panel rounded-2xl overflow-hidden">
        {/* Tab Headers */}
        <div className="flex border-b border-slate-800">
          <button 
            className={`flex-1 py-4 font-semibold text-center transition ${activeTab === 'manual' ? 'text-primary border-b-2 border-primary bg-primary/5' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'}`}
            onClick={() => setActiveTab('manual')}
          >
            <PenTool size={18} className="inline mr-2 mb-1" />
            Manual Entry
          </button>
          <button 
            className={`flex-1 py-4 font-semibold text-center transition ${activeTab === 'csv' ? 'text-primary border-b-2 border-primary bg-primary/5' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'}`}
            onClick={() => setActiveTab('csv')}
          >
            <LayoutList size={18} className="inline mr-2 mb-1" />
            Batch CSV Upload
          </button>
        </div>

        <div className="p-8">
          <h1 className="text-2xl font-bold text-white mb-2">
            {activeTab === 'manual' ? 'Ingest Manual Transaction' : 'Bulk Data Ingestion'}
          </h1>
          <p className="text-slate-400 mb-8">
            {activeTab === 'manual' ? 'Record a payment to feed into the Intelligence Engine.' : 'Upload historical bank statements to instantly train the Intelligence Engine.'}
          </p>

          {isSuccess && (
             <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="bg-success/10 border border-success/30 text-success px-4 py-3 rounded-lg mb-6 flex items-center">
              <CheckCircle size={18} className="mr-2" />
              {activeTab === 'manual' ? 'Transaction successfully dispatched!' : `Successfully processed ${batchTotal} records!`}
            </motion.div>
          )}

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-500 px-4 py-3 rounded-lg mb-6 flex items-center">
              {error}
            </div>
          )}

          {activeTab === 'manual' ? (
            <form onSubmit={handleManualSubmit} className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-slate-400 text-sm font-medium mb-1">Merchant Name</label>
                  <input required type="text" value={formData.merchant} onChange={(e) => setFormData({...formData, merchant: e.target.value})} className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-2.5 px-4 focus:outline-none focus:border-primary transition" placeholder="e.g. Netflix, AWS" />
                </div>
                <div>
                  <label className="block text-slate-400 text-sm font-medium mb-1">Amount (₹)</label>
                  <input required type="number" step="0.01" value={formData.amount} onChange={(e) => setFormData({...formData, amount: e.target.value})} className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-2.5 px-4 focus:outline-none focus:border-primary transition" placeholder="649.00" />
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-slate-400 text-sm font-medium mb-1">Category</label>
                  <input required type="text" value={formData.category} onChange={(e) => setFormData({...formData, category: e.target.value})} className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-2.5 px-4 focus:outline-none focus:border-primary transition" placeholder="e.g. Entertainment" />
                </div>
                <div>
                  <label className="block text-slate-400 text-sm font-medium mb-1">Date & Time</label>
                  <input required type="datetime-local" value={formData.timestamp} onChange={(e) => setFormData({...formData, timestamp: e.target.value})} className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-2.5 px-4 focus:outline-none focus:border-primary transition" />
                </div>
              </div>
              <div className="pt-4">
                <button type="submit" disabled={isLoading} className={`px-6 py-2.5 font-medium rounded-lg transition shadow-lg ${isLoading ? 'bg-slate-700 text-slate-400 cursor-not-allowed border-slate-600' : 'bg-primary hover:bg-blue-600 text-white shadow-primary/20'}`}>
                  {isLoading ? 'Dispatching...' : 'Dispatch to Intelligence Engine'}
                </button>
              </div>
            </form>
          ) : (
            <div className="space-y-6">
              {!csvFile ? (
                <div {...getRootProps()} className={`border-2 border-dashed rounded-xl p-12 text-center transition cursor-pointer ${isDragActive ? 'border-primary bg-primary/5' : 'border-slate-700 hover:border-slate-500 bg-slate-900/50'}`}>
                  <input {...getInputProps()} />
                  <UploadCloud size={48} className={`mx-auto mb-4 ${isDragActive ? 'text-primary' : 'text-slate-500'}`} />
                  <p className="text-white font-medium mb-1">Drag and drop your statement here</p>
                  <p className="text-sm text-slate-400">or click to browse from your computer (CSV only)</p>
                </div>
              ) : (
                <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="border border-slate-700 bg-slate-900/80 rounded-xl p-6 relative">
                  <button onClick={() => !isLoading && setCsvFile(null)} className="absolute top-4 right-4 text-slate-400 hover:text-white" disabled={isLoading}><X size={20} /></button>
                  <div className="flex items-center mb-6">
                    <div className="p-3 bg-primary/10 rounded-lg text-primary mr-4"><FileText size={24} /></div>
                    <div>
                      <h3 className="font-semibold text-white">{csvFile.name}</h3>
                      <p className="text-sm text-slate-400">{(csvFile.size / 1024).toFixed(1)} KB • Ready to process</p>
                    </div>
                  </div>
                  
                  {isLoading && batchTotal > 0 && (
                     <div className="mb-6">
                        <div className="flex justify-between text-sm mb-2">
                           <span className="text-slate-300">Processing records...</span>
                           <span className="text-primary font-mono">{batchProgress} / {batchTotal}</span>
                        </div>
                        <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
                           <motion.div className="bg-primary h-2.5" initial={{ width: 0 }} animate={{ width: `${(batchProgress/batchTotal)*100}%` }} transition={{ ease: "linear" }} />
                        </div>
                     </div>
                  )}

                  <button onClick={handleCsvSubmit} disabled={isLoading} className={`w-full py-3 font-medium rounded-lg transition shadow-lg ${isLoading ? 'bg-slate-700 text-slate-400 cursor-not-allowed' : 'bg-primary hover:bg-blue-600 text-white shadow-primary/20'}`}>
                    {isLoading ? `Injecting (${batchProgress}/${batchTotal})...` : 'Process Batch Statement'}
                  </button>
                </motion.div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AddTransaction;
