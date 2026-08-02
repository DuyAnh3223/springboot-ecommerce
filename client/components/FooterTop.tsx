import React from 'react'
import { Clock, Mail, MapPin, Phone } from 'lucide-react';

interface ContactItemData{
  title: string;
  subtitle: string;
  icon: React.ReactNode;
}

const data: ContactItemData[] = [
  {
    title: "Cửa hàng ABTechZone",
    subtitle: "TP. Hồ Chí Minh, Việt Nam",
    icon: (
      <MapPin className="h-6 w-6 text-gray-600 group-hover:text-primary transition-colors" />
    ),
  },
  {
    title: "Hotline hỗ trợ",
    subtitle: "+84 958 648 597",
    icon: (
      <Phone className="h-6 w-6 text-gray-600 group-hover:text-primary transition-colors" />
    ),
  },
  {
    title: "Giờ làm việc",
    subtitle: "Thứ 2 - Chủ Nhật: 8:00 - 21:00",
    icon: (
      <Clock className="h-6 w-6 text-gray-600 group-hover:text-primary transition-colors" />
    ),
  },
  {
    title: "Email tư vấn",
    subtitle: "abtechzone@gmail.com",
    icon: (
      <Mail className="h-6 w-6 text-gray-600 group-hover:text-primary transition-colors " />
    ),
  },
];
const FooterTop = () => {
  return (
    <div className='grid grid-cols-2 lg:grid-cols-4 gap-8 border-b'>
        {data?.map((item, index)=>(
           <div key={index} className='flex items-center gap-3 group hover:bg-gray-50 p-4 transition-colors hoverEffect'> 
           {item?.icon} 
           <div>
            <h3 className='font-semibold text-gray-900 group-hover:text-black hoverEffect'>{item?.title}</h3>
            <p className='text-gray-600 text-sm mt-1 group-hover:text-gray-900 hoverEffect'>{item?.subtitle}</p>
           </div>
           </div>
        ))}
    </div>
  )
}




export default FooterTop