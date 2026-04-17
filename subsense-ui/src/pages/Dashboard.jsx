import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Activity, CreditCard, TrendingDown, ArrowUpRight, Zap, PieChart as PieIcon, List as ListIcon, ShieldCheck, AlertTriangle, Sparkles, TrendingUp } from 'lucide-react';
import axios from 'axios';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer, Tooltip as RechartsTooltip, Cell, PieChart, Pie, Legend, ReferenceLine } from 'recharts';

const generateMockAIEngine = (subs) => {
  const recs = [];
  subs.forEach(sub => {
     const up = sub.merchant.toUpperCase();
     if (up.includes('GYM') || up.includes('FITNESS')) {
        recs.push({ merchant: sub.merchant, type: 'CANCEL', exp: `Fitness subscriptions hold 80% abandonment. Consider canceling to save ₹${sub.avgAmount} monthly.`, cost: sub.avgAmount });
     }
     if (sub.avgAmount > 5000) {
        recs.push({ merchant: sub.merchant, type: 'DOWNGRADE', exp: `High spend detected. Optimizing internal seats can reduce your burn.`, cost: sub.avgAmount * 0.3 });
     }
     if (up.includes('ADOBE') || up.includes('CLOUD')) {
         recs.push({ merchant: sub.merchant, type: 'DOWNGRADE', exp: `Check eligibility for academic or startup rate reductions on ${sub.merchant}.`, cost: sub.avgAmount * 0.6 });
     }
  });

  const ent = subs.filter(s => ['NETFLIX', 'SPOTIFY', 'HULU', 'AMAZON', 'YOUTUBE', 'PRIME'].some(v => s.merchant.toUpperCase().includes(v)));
  if (ent.length > 2) {
      recs.push({ merchant: 'Streaming Sprawl', type: 'CANCEL', exp: `You are paying for ${ent.length} streaming services simultaneously. Rotating them could save significant capital.`, cost: 1500 });
  }
  return recs;
};

const generateForecastTimeline = (spend) => {
    const data = [];
    let current = spend * 0.8; 
    const months = ['Oct', 'Nov', 'Dec', 'Jan', 'Feb', 'Mar', 'Apr (Est)'];
    months.forEach((m) => {
        data.push({ name: m, burn: Math.max(0, Math.round(current)) });
        current += (Math.random() * 800 - 300); 
    });
    return data;
}

const COLORS = ['#00E5FF', '#00B3FF', '#B026FF', '#FF2A55', '#FFD600'];

const formatCurrencyYAxis = (tickItem) => {
    if (tickItem === 0) return '0';
    if (tickItem >= 1000) return `₹${(tickItem / 1000).toFixed(0)}k`;
    return `₹${tickItem}`;
};

const Dashboard = () => {
  const { user } = useAuthStore();
  const [subscriptions, setSubscriptions] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  
  // Wealth Simulator State
  const [toggledOffSubs, setToggledOffSubs] = useState(new Set());
  
  const SUBSCRIPTION_API = 'http://localhost:8082';

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      try {
        if (!user?.userId) return;
        const subsRes = await axios.get(`${SUBSCRIPTION_API}/subscriptions/search?userId=${user.userId}`, { headers: { Authorization: `Bearer ${useAuthStore.getState().accessToken}` } }).catch(() => ({ data: [] }));
        const subs = subsRes.data || [];
        // Dedup and normalize
        const uniqueSubs = Array.from(new Map(subs.map(item => [item.merchant, item])).values());
        setSubscriptions(uniqueSubs);
        setRecommendations(generateMockAIEngine(uniqueSubs));
      } catch (error) {
        console.error("Dashboard data fetch failed", error);
      } finally { setIsLoading(false); }
    };
    fetchData();
  }, [user]);

  const filteredSubs = subscriptions.filter(sub => sub.merchant.toLowerCase().includes(searchTerm.toLowerCase()));
  const totalMonthlySpend = subscriptions.reduce((sum, sub) => sum + sub.avgAmount, 0);
  const potentialSavings = recommendations.reduce((sum, r) => sum + r.cost, 0);

  const timelineData = generateForecastTimeline(totalMonthlySpend);
  const averageBurn = Math.round(timelineData.reduce((acc, d) => acc + d.burn, 0) / (timelineData.length || 1));
  
  const latestBurn = timelineData[timelineData.length - 1]?.burn || 0;
  const prevBurn = timelineData[timelineData.length - 2]?.burn || 0;
  const burnDiff = latestBurn - prevBurn;
  
  const categories = {};
  subscriptions.forEach(s => { categories[s.category] = (categories[s.category] || 0) + s.avgAmount; });
  const barData = Object.keys(categories).map(k => ({ name: k, amount: categories[k] })).sort((a,b)=>b.amount-a.amount);

  // Wealth Simulator Math
  const monthlyFreed = subscriptions.reduce((acc, sub) => {
    if (toggledOffSubs.has(sub.merchant)) return acc + sub.avgAmount;
    return acc;
  }, 0);
  const yearlyFreed = monthlyFreed * 12;
  const fiveYearFreed = monthlyFreed * 12 * 5;

  const handleToggle = (merchant) => {
      setToggledOffSubs(prev => {
          const next = new Set(prev);
          if (next.has(merchant)) next.delete(merchant);
          else next.add(merchant);
          return next;
      });
  };

  return (
    <div className="space-y-12 animate-in fade-in duration-1000 pb-32 relative z-10 w-full min-h-screen font-sans">
      
      {/* Aurora FinTech Header */}
      <header className="mb-14 mt-8 relative group">
        <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-foreground tracking-tight leading-tight">
          Welcome back, <span className="text-gradient font-extrabold">{user?.name?.split(' ')[0] || 'User'}.</span>
        </h1>
        <p className="text-muted mt-4 text-lg font-medium tracking-wide max-w-2xl">
          Here is your autonomous financial telemetry. We’ve analyzed your recurring capital flows and identified key optimization opportunities.
        </p>
      </header>

      {/* Futuristic Tab System */}
      <div className="flex space-x-1 border-b border-white/[0.04] pb-px overflow-x-auto no-scrollbar relative z-30">
        {[
            { id: 'overview', icon: PieIcon, text: 'Overview' },
            { id: 'forecast', icon: TrendingDown, text: 'Velocity' },
            { id: 'matrix', icon: Sparkles, text: 'Wealth Simulator' } // Renamed to emphasize new feature
        ].map(tab => (
            <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`relative px-6 py-3 text-sm font-semibold tracking-wide transition-colors flex items-center rounded-t-lg mx-1 ${activeTab === tab.id ? 'text-foreground bg-white/[0.03]' : 'text-muted hover:text-white/80 hover:bg-white/[0.01]'}`}>
                <tab.icon className={`w-4 h-4 mr-2 ${activeTab === tab.id ? 'text-primary' : ''}`} /> {tab.text}
                {activeTab === tab.id && <motion.div layoutId="activeTab" className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary shadow-glow-cyan" />}
            </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        
        {/* OVERVIEW TAB */}
        {activeTab === 'overview' && (
          <motion.div key="overview" initial={{opacity: 0, y: 15}} animate={{opacity: 1, y: 0}} exit={{opacity: 0, scale: 0.98}} transition={{duration: 0.4}} className="space-y-10">
            
            {/* HUD SUMMARY CARDS */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                
                {/* Spend Card */}
                <motion.div whileHover={{ scale: 1.01 }} className="bg-surfaceElevated rounded-3xl p-7 relative overflow-hidden border border-white/[0.05] shadow-xl group">
                   <div className="flex justify-between items-start mb-6 relative z-10">
                       <div className="p-3 bg-white/[0.02] rounded-2xl border border-white/[0.05] group-hover:bg-white/[0.05] transition-colors"><Activity className="text-foreground w-6 h-6" /></div>
                       <div className="flex items-center text-xs font-semibold text-danger bg-danger/10 px-3 py-1 rounded-full"><TrendingUp className="w-3 h-3 mr-1"/> Active Burn</div>
                   </div>
                   <p className="text-muted font-medium text-sm mb-1 relative z-10">Total Monthly Burn</p>
                   <h2 className="text-4xl font-extrabold text-white relative z-10 tracking-tight">₹{totalMonthlySpend.toLocaleString('en-IN', {maximumFractionDigits:0})}</h2>
                </motion.div>

                {/* Subscriptions Card */}
                <motion.div whileHover={{ scale: 1.01 }} className="bg-surfaceElevated rounded-3xl p-7 relative overflow-hidden border border-white/[0.05] shadow-xl group">
                   <div className="flex justify-between items-start mb-6 relative z-10">
                       <div className="p-3 bg-white/[0.02] rounded-2xl border border-white/[0.05] group-hover:bg-white/[0.05] transition-colors"><CreditCard className="text-foreground w-6 h-6" /></div>
                   </div>
                   <p className="text-muted font-medium text-sm mb-1 relative z-10">Active Contracts</p>
                   <h2 className="text-4xl font-extrabold text-white relative z-10 tracking-tight">{subscriptions.length}</h2>
                </motion.div>

                {/* AI Savings Card */}
                <motion.div whileHover={{ scale: 1.01 }} className="bg-gradient-to-br from-surfaceElevated to-surfaceElevated rounded-3xl p-7 relative overflow-hidden border border-primary/20 shadow-glow-cyan group cursor-pointer" onClick={() => setActiveTab('matrix')}>
                   <div className="absolute inset-0 bg-primary/5 group-hover:bg-primary/10 transition-colors"></div>
                   <div className="absolute -right-10 -bottom-10 opacity-20"><Zap className="w-48 h-48 text-primary" /></div>
                   <div className="flex justify-between items-start mb-6 relative z-10">
                       <div className="p-3 bg-primary/20 rounded-2xl border border-primary/30 text-primary shadow-glow-cyan"><Zap className="w-6 h-6" /></div>
                       <div className="flex items-center text-[10px] font-bold uppercase tracking-wider text-primary border border-primary/30 px-3 py-1 rounded-full bg-primary/10">
                          <Sparkles size={12} className="mr-1" /> Optima Try
                       </div>
                   </div>
                   <p className="text-primary/80 font-medium text-sm mb-1 relative z-10">Potential AI Savings</p>
                   <h2 className="text-4xl font-extrabold text-white relative z-10 tracking-tight">₹{potentialSavings.toLocaleString('en-IN', {maximumFractionDigits:0})}</h2>
                </motion.div>

            </div>

            {/* Smart Insights Matrix */}
            <div className="pt-6">
               <h2 className="text-2xl font-bold text-foreground mb-6 flex items-center tracking-tight">
                 Smart Optimization Engine
               </h2>
               <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                 {recommendations.length > 0 ? recommendations.map((rec, idx) => {
                     const isCancel = rec.type === 'CANCEL';
                     const accentColor = isCancel ? 'danger' : 'warning';
                     const Icon = isCancel ? AlertTriangle : ArrowUpRight;

                     return (
                         <motion.div key={idx} initial={{opacity: 0, y: 15}} animate={{opacity: 1, y: 0}} transition={{delay: 0.1 * idx}} 
                             className="bg-surfaceElevated p-6 rounded-2xl relative overflow-hidden border border-white/[0.03] hover:border-white/[0.08] transition-all group">
                           <div className={`flex items-center justify-between mb-4`}>
                              <div className="flex items-center space-x-3">
                                 <div className={`p-2 rounded-xl bg-${accentColor}/10 text-${accentColor}`}><Icon size={18}/></div>
                                 <h3 className="text-lg font-bold text-white tracking-tight">{rec.merchant}</h3>
                              </div>
                              <div className={`text-[10px] font-extrabold tracking-widest uppercase text-${accentColor} bg-${accentColor}/10 px-2 py-1 rounded border border-${accentColor}/20`}>
                                {rec.type}
                              </div>
                           </div>
                           <p className="text-muted text-sm font-medium leading-relaxed mb-4">{rec.exp}</p>
                           <div className={`text-sm font-semibold text-${accentColor} mt-auto`}>Est. Impact: ₹{Math.round(rec.cost).toLocaleString()} /mo</div>
                         </motion.div>
                     )
                 }) : <div className="col-span-full p-10 text-center text-muted bg-surfaceElevated border border-dashed border-white/10 rounded-2xl font-medium tracking-wide">Your contracts are fully optimized. No anomalies detected.</div>}
               </div>
            </div>
          </motion.div>
        )}

        {/* FORECAST TAB */}
        {activeTab === 'forecast' && (
            <motion.div key="forecast" initial={{opacity: 0, y: 15}} animate={{opacity: 1, y: 0}} exit={{opacity: 0, scale: 0.98}} transition={{duration: 0.4}} className="space-y-6">
              
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Spend Velocity Bar Chart */}
                  <div className="bg-surfaceElevated p-7 md:p-8 rounded-[2rem] border border-white/[0.05] shadow-xl flex flex-col">
                      <div className="mb-4">
                         <h3 className="text-lg font-bold text-white tracking-tight">Spend Velocity</h3>
                         <p className="text-sm font-medium text-muted">A 6-month historical trajectory of your capital exit vectors.</p>
                      </div>
                      
                      {/* Dynamic Insight */}
                      <div className="mb-6 bg-primary/10 border border-primary/20 rounded-xl p-4 flex items-center">
                         <Activity className="text-primary w-5 h-5 mr-3 flex-shrink-0" />
                         <span className="text-sm text-primary font-medium tracking-wide">
                           {burnDiff > 0 
                             ? `Your burn rate is trending UP by ₹${Math.abs(burnDiff).toLocaleString('en-IN')} this period. Consider auditing subscriptions.` 
                             : burnDiff < 0 
                             ? `Excellent! Your burn rate dropped by ₹${Math.abs(burnDiff).toLocaleString('en-IN')}.` 
                             : `Your spend velocity is perfectly stable across periods.`}
                         </span>
                      </div>

                      <div className="h-72 w-full mt-auto">
                        <ResponsiveContainer>
                            <BarChart data={timelineData} margin={{top: 20, right: 10, left: -10, bottom: 0}}>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.02)" vertical={false} />
                                <XAxis dataKey="name" stroke="none" tick={{fill: '#94A3B8', fontSize: 12, fontWeight: 500}} dy={10}/>
                                <YAxis stroke="none" tick={{fill: '#94A3B8', fontSize: 12, fontWeight: 500}} tickFormatter={formatCurrencyYAxis} dx={-10}/>
                                <RechartsTooltip contentStyle={{backgroundColor: '#0A0A0F', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '12px', boxShadow: '0 4px 20px rgba(0,0,0,0.5)'}} itemStyle={{color: '#00E5FF', fontWeight: 600}} cursor={{fill: 'rgba(255,255,255,0.02)'}} formatter={(val) => [`₹${val}`, 'Burn']} />
                                <ReferenceLine y={averageBurn} stroke="#B026FF" strokeDasharray="3 3" label={{ position: 'top', value: 'Avg Baseline', fill: '#B026FF', fontSize: 11, fontWeight: 700 }} />
                                <Bar dataKey="burn" fill="#00E5FF" radius={[4, 4, 0, 0]} barSize={28} />
                            </BarChart>
                        </ResponsiveContainer>
                      </div>
                  </div>
                  
                  {/* Category Donut Chart */}
                  <div className="bg-surfaceElevated p-7 md:p-8 rounded-[2rem] border border-white/[0.05] shadow-xl flex flex-col">
                      <div className="mb-4">
                         <h3 className="text-lg font-bold text-white tracking-tight">Category Vectors</h3>
                         <p className="text-sm font-medium text-muted">Active burn segmented by explicit infrastructure categories.</p>
                      </div>
                      <div className="flex-grow w-full relative">
                        <div className="absolute inset-0 flex flex-col justify-center items-center pointer-events-none top-[-20px]">
                           <span className="text-xs font-bold text-muted uppercase tracking-widest">Total Active</span>
                           <span className="text-2xl font-black text-white">₹{totalMonthlySpend.toLocaleString('en-IN', {maximumFractionDigits:0})}</span>
                        </div>
                        <ResponsiveContainer>
                            <PieChart>
                                <Pie data={barData} dataKey="amount" nameKey="name" cx="50%" cy="50%" innerRadius={85} outerRadius={115} stroke="none" paddingAngle={4} minAngle={15}>
                                  {barData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                  ))}
                                </Pie>
                                <RechartsTooltip formatter={(val) => [`₹${val}`, 'Spend']} contentStyle={{backgroundColor: '#0A0A0F', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '12px'}} itemStyle={{color: '#fff', fontWeight: 600}} />
                                <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{fontSize: '12px', color: '#94A3B8', fontWeight: 500}} />
                            </PieChart>
                        </ResponsiveContainer>
                      </div>
                  </div>
              </div>
            </motion.div>
        )}

        {/* WEALTH SIMULATOR / MATRIX TAB */}
        {activeTab === 'matrix' && (
            <motion.div key="matrix" initial={{opacity: 0, y: 15}} animate={{opacity: 1, y: 0}} exit={{opacity: 0, scale: 0.98}} transition={{duration: 0.4}}>
              
              {/* INTERACTIVE WEALTH BANNER */}
              <div className="bg-gradient-to-r from-[#0A0A0F] to-[#12121A] border border-secondary/30 rounded-3xl p-6 mb-8 flex flex-col md:flex-row justify-between items-center shadow-glow-purple relative overflow-hidden group">
                  <div className="absolute inset-0 bg-secondary/5 group-hover:bg-secondary/10 transition-colors pointer-events-none"></div>
                  <div className="relative z-10 flex items-center space-x-6">
                     <div className="p-4 bg-secondary/20 rounded-2xl text-secondary shadow-glow-purple"><Sparkles className="w-8 h-8"/></div>
                     <div>
                        <h2 className="text-2xl font-black text-white tracking-tight">Future Wealth Simulator</h2>
                        <p className="text-sm font-medium text-muted mt-1">Toggle active contracts below to simulate financial impact.</p>
                     </div>
                  </div>
                  <div className="relative z-10 flex space-x-8 mt-6 md:mt-0 bg-[#030305]/50 px-8 py-4 rounded-2xl border border-white/5">
                      <div className="text-center">
                         <div className="text-[10px] font-extrabold uppercase tracking-widest text-muted mb-1">Capital Freed (1 YR)</div>
                         <div className="text-2xl font-black text-white">₹{yearlyFreed.toLocaleString('en-IN')}</div>
                      </div>
                      <div className="w-px bg-white/10"></div>
                      <div className="text-center">
                         <div className="text-[10px] font-extrabold uppercase tracking-widest text-secondary mb-1">Capital Freed (5 YR)</div>
                         <div className="text-3xl font-black text-secondary drop-shadow-[0_0_12px_rgba(176,38,255,0.8)]">₹{fiveYearFreed.toLocaleString('en-IN')}</div>
                      </div>
                  </div>
              </div>

              <div className="flex flex-col sm:flex-row justify-between items-center mb-6">
                 <div>
                    <h2 className="text-xl font-bold text-foreground tracking-tight">Your Registry</h2>
                 </div>
                 <div className="relative w-full sm:w-80 mt-4 sm:mt-0">
                    <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-muted w-4 h-4" />
                    <input type="text" placeholder="Search registry..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="w-full bg-surfaceElevated border border-white/[0.05] text-sm text-foreground rounded-xl py-3 pl-11 pr-4 focus:outline-none focus:border-primary/40 focus:ring-1 focus:ring-primary/40 transition-all font-medium placeholder:text-muted" />
                 </div>
              </div>
              
              <div className="bg-surfaceElevated rounded-[1.5rem] overflow-hidden border border-white/[0.05] shadow-xl">
                  {filteredSubs.length > 0 ? (
                      <div className="grid grid-cols-1 divide-y divide-white/[0.02]">
                          {filteredSubs.map((sub, idx) => {
                             const isHighTrust = sub.confidenceScore > 80;
                             const isToggledOff = toggledOffSubs.has(sub.merchant);

                             return (
                              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: idx * 0.03 }} key={sub.id || idx} 
                                   className={`flex items-center justify-between p-5 transition-colors ${isToggledOff ? 'bg-black/40 opacity-70 border-l-[3px] border-l-secondary/50' : 'hover:bg-white/[0.02]'}`}>
                                  
                                  <div className="flex items-center space-x-5 w-1/3">
                                      {/* Custom Toggle Switch */}
                                      <button 
                                        onClick={() => handleToggle(sub.merchant)}
                                        className={`w-12 h-6 rounded-full p-1 transition-colors flex ${isToggledOff ? 'bg-surface border border-white/10 justify-start' : 'bg-primary justify-end'}`}
                                      >
                                        <motion.div layout className={`w-4 h-4 rounded-full ${isToggledOff ? 'bg-muted' : 'bg-background shadow-md'}`}></motion.div>
                                      </button>
                                      <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-white ${isHighTrust ? 'bg-secondary/40 border border-secondary/50' : 'bg-surfaceElevated border border-white/10'}`}>
                                         {sub.merchant.charAt(0).toUpperCase()}
                                      </div>
                                      <div>
                                          <div className={`font-bold text-base tracking-tight ${isToggledOff ? 'text-muted line-through' : 'text-white'}`}>{sub.merchant}</div>
                                          <div className="text-xs font-medium text-muted mt-0.5">{sub.category}</div>
                                      </div>
                                  </div>

                                  <div className="w-1/4 text-center">
                                      <div className={`text-base font-bold ${isToggledOff ? 'text-muted' : 'text-foreground'}`}>₹{sub.avgAmount.toLocaleString('en-IN', {minimumFractionDigits: 0})}</div>
                                      <div className="text-[10px] font-bold text-muted uppercase tracking-wider mt-1">{sub.billingCycle}</div>
                                  </div>

                                  <div className="w-1/3 flex justify-end">
                                      <span className={`inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-bold tracking-wide space-x-1.5 ${ isToggledOff ? 'bg-surface text-muted border border-white/5' : isHighTrust ? 'bg-success/10 text-success' : 'bg-warning/10 text-warning' }`}> 
                                         <ShieldCheck size={14}/>
                                         <span>{isToggledOff ? 'Inactive' : `${sub.confidenceScore.toFixed(0)}% Trust`}</span>
                                      </span>
                                  </div>
                              </motion.div>
                             )
                          })}
                      </div>
                  ) : (
                      <div className="p-12 text-center text-muted font-medium">No contracts found in registry.</div>
                  )}
              </div>
            </motion.div>
        )}

      </AnimatePresence>
    </div>
  );
};

export default Dashboard;
